# CustomBond — Application & Code Flow Explanation

## What is CustomBond?

**CustomBond** is a Spring Boot 3.2.5 REST API that acts as a **middleware orchestrator** between vendors and an external insurance service (DXC). Its job is to execute insurance issuance workflows — accepting vendor requests, running the appropriate pipeline steps, and returning results asynchronously.

---

## Overall Request Flow (Bird's Eye View)

```
Vendor
  │
  ▼
POST /vendor/issue  (multipart: JSON + optional file)
  │
  ▼
VendorController  ──► Returns HTTP 202 immediately
  │                    (VendorIssuanceAcknowledgement)
  ▼
VendorPipelineAsyncService  (@Async, thread pool)
  │
  ▼
IssuancePipeline.execute()
  │
  ├── Step 1: lookup in registry → execute(context)
  ├── Step 2: lookup in registry → execute(context)
  ├── Step N: ...
  │
  ▼
VendorPipelineResult  ──► Logged to DB
                      ──► POSTed to vendor's callbackUrl (if provided)
```

---

## The Pipeline Architecture (Core of the System)

### 1. Step Definition — `PipelineStepType.java`

An enum that declares every possible step the system knows about:

```
CHECK_BLACK_LIST
QUOTE_PREPARATION
NAFEZA_QUOTE_PREPARATION
UPLOAD_DOCUMENT
NAFEZA_UPLOAD_DOCUMENTS
ISSUE_QUOTE
ISSUE_POLICY
```

### 2. Step Interface — `PipelineStep.java`

```java
interface PipelineStep {
    PipelineStepType getType();
    void execute(IssuancePipelineContext context) throws PipelineStepException;
}
```

Every step is a `@Component` bean implementing this interface.

### 3. Step Registry — Built inside `IssuancePipeline.java`

At startup, Spring auto-discovers all `PipelineStep` beans and builds a registry:

```java
Map<PipelineStepType, PipelineStep> stepRegistry = ...;
// e.g., { CHECK_BLACK_LIST → CheckBlackListStep, ISSUE_QUOTE → IssueQuoteStep, ... }
```

### 4. Pipeline Execution — `IssuancePipeline.java`

```java
for (PipelineStepType stepType : context.request.steps) {
    PipelineStep step = stepRegistry.get(stepType);
    step.execute(context);  // mutates shared context
    // any PipelineStepException aborts the pipeline
}
```

### 5. Shared Context — `IssuancePipelineContext.java`

A single mutable object passed through all steps. Steps read from it and write to it:

| Field | Written By | Read By |
|---|---|---|
| `blackListResult` (contactKey) | CHECK_BLACK_LIST | NAFEZA_QUOTE_PREPARATION |
| `policyNo` | QUOTE_PREPARATION / NAFEZA_QUOTE_PREPARATION | ISSUE_QUOTE, ISSUE_POLICY |
| `policyKey` | QUOTE_PREPARATION / NAFEZA_QUOTE_PREPARATION | UPLOAD_DOCUMENT |
| `nafezaRequest` | Set before pipeline starts | NAFEZA_* steps |
| `successful`, `failedStep` | Pipeline itself | Result reporting |

---

## Step-by-Step Dependency Chain

```
CHECK_BLACK_LIST
  └─ writes: blackListResult.contactKey
                       ↓ used by
NAFEZA_QUOTE_PREPARATION  (OR)  QUOTE_PREPARATION
  └─ writes: policyNo, policyKey
                    ↓ used by
        UPLOAD_DOCUMENT / NAFEZA_UPLOAD_DOCUMENTS
        ISSUE_QUOTE  ──► DXC Issue Quote API
        ISSUE_POLICY ──► DXC Issue Policy API
```

---

## The Two Vendor Flow Variants

### Standard Vendor Flow

```json
POST /vendor/issue
{
  "vendorId": "V001",
  "steps": ["CHECK_BLACK_LIST", "QUOTE_PREPARATION", "UPLOAD_DOCUMENT", "ISSUE_QUOTE", "ISSUE_POLICY"],
  "blackList": { "..." },
  "quotePreparation": { "..." },
  "upload": { "..." }
}
```

### NAFEZA Vendor Flow

```json
POST /vendor/issue/nafeza
{
  "vendorId": "V002",
  "steps": ["CHECK_BLACK_LIST", "NAFEZA_QUOTE_PREPARATION", "NAFEZA_UPLOAD_DOCUMENTS", "ISSUE_QUOTE", "ISSUE_POLICY"],
  "blackList": { "..." },
  "nafezaQuoteData": { "..." }
}
```

> **Key difference:** The NAFEZA flow has no `insured` field in the quote data. Instead, `insured` is automatically resolved from the blacklist `contactKey` written by `CHECK_BLACK_LIST` into the shared context.

---

## Per-Step Input Requirements

| Step | Input DTO | Notes |
|---|---|---|
| `CHECK_BLACK_LIST` | `VendorBlackListData` | Requires stakeholderName + (taxId OR nationalId OR contactKey) |
| `QUOTE_PREPARATION` | `DXCQuotePreparationRequest` | Fully pre-built by vendor; includes insured contactKey |
| `NAFEZA_QUOTE_PREPARATION` | `NafezaQuotePreparationData` | Same structure but WITHOUT insured — resolved from blacklist |
| `UPLOAD_DOCUMENT` | `VendorUploadData` | entityKey, documentType, optional instnceKey |
| `NAFEZA_UPLOAD_DOCUMENTS` | `VendorUploadData` | Same structure; reads from nafezaRequest context |
| `ISSUE_QUOTE` | _(none)_ | Uses policyNo from context or request.policyNo fallback |
| `ISSUE_POLICY` | _(none)_ | Uses policyNo from context or request.policyNo fallback |

---

## Does It Dynamically Support Any Vendor With Different Steps?

**Yes — fully.** Here is the proof:

### 1. Steps are data-driven, not hard-coded

The pipeline executes whatever is in `request.steps[]`. There is no `if/else` routing and no hard-coded sequences anywhere in the orchestrator.

### 2. Vendors can use any subset of steps

A vendor that already knows the `policyNo` can skip `QUOTE_PREPARATION` entirely:

```json
{
  "steps": ["ISSUE_QUOTE", "ISSUE_POLICY"],
  "policyNo": "POL-2024-99999"
}
```

### 3. Vendors can use any ordering

Steps execute in exactly the order the vendor provides in the `steps` array.

### 4. Adding a new step requires zero changes to existing code

1. Add a new value to `PipelineStepType` enum
2. Create a new `@Component` class implementing `PipelineStep`

Spring auto-discovers and registers it. The pipeline picks it up the next time any vendor includes that step name.

### 5. No vendor is hard-coded in the system

There is no vendor-specific routing logic. A "vendor" is simply any HTTP client that POSTs a valid request with a `steps` list.

---

## Async Model & Response Strategy

| Phase | What Happens |
|---|---|
| HTTP request received | Controller validates, extracts file bytes, stores request |
| HTTP 202 returned | Immediately — vendor gets `requestId`, `acceptedAt`, and `steps` echoed back |
| Background execution | Thread pool (`core=5, max=10, queue=100`) runs the pipeline |
| Completion | Result saved to DB; if `callbackUrl` provided, a POST is sent there |

Vendors are never blocked waiting for DXC. They receive a correlation `requestId` upfront and get the final result via webhook callback.

### Response: Acknowledgement (HTTP 202)

```json
{
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "vendorRequestId": "REQ-2024-001",
  "message": "Request accepted. Processing pipeline asynchronously.",
  "acceptedAt": "2024-01-15T10:30:00",
  "steps": ["CHECK_BLACK_LIST", "QUOTE_PREPARATION", "ISSUE_QUOTE", "ISSUE_POLICY"]
}
```

### Response: Final Result (via callbackUrl)

```json
{
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "vendorId": "V001",
  "success": true,
  "policyNo": "POL-2024-00001",
  "completedAt": "2024-01-15T10:30:15"
}
```

---

## Error Handling

Any step failure throws `PipelineStepException`. The pipeline catches it, aborts all remaining steps, and records the failure:

```json
{
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "vendorId": "V001",
  "success": false,
  "failedStep": "QuotePreparation",
  "failureMessage": "DXC rejected request (400) – Invalid insured contact",
  "completedAt": "2024-01-15T10:30:15"
}
```

Unchecked exceptions (e.g., `NullPointerException`) are also caught and treated as fatal failures with the same abortion behavior.

---

## Project File Structure

```
src/main/java/com/custombond/
├── pipeline/
│   ├── PipelineStepType.java          # Enum: all 7 step types
│   ├── PipelineStep.java              # Interface every step implements
│   ├── IssuancePipeline.java          # Orchestrator: registry + execution loop
│   ├── IssuancePipelineContext.java   # Shared mutable context passed between steps
│   ├── PipelineStepException.java     # Expected failure exception
│   └── steps/
│       ├── CheckBlackListStep.java
│       ├── QuotePreparationStep.java
│       ├── UploadDocumentStep.java
│       ├── IssueQuoteStep.java
│       ├── IssuePolicyStep.java
│       ├── NafezaQuotePreparationStep.java
│       └── NafezaUploadDocumentsStep.java
├── controller/
│   └── VendorController.java          # POST /vendor/issue, POST /vendor/issue/nafeza
├── service/
│   ├── VendorPipelineAsyncService.java
│   ├── DXC_BlackList_Service.java
│   ├── DXCQuotePreparationService.java
│   ├── DXC_UploadDocument_Service.java
│   ├── DXC_IssueQuote_Service.java
│   └── DXC_IssuePolicy_Service.java
├── dto/
│   ├── request/
│   │   ├── VendorIssuanceRequest.java
│   │   ├── NafezaIssuanceRequest.java
│   │   ├── VendorBlackListData.java
│   │   ├── DXCQuotePreparationRequest.java
│   │   ├── NafezaQuotePreparationData.java
│   │   └── VendorUploadData.java
│   └── response/
│       ├── VendorIssuanceAcknowledgement.java
│       └── VendorPipelineResult.java
├── config/
│   ├── AsyncConfig.java               # Thread pool (core=5, max=10, queue=100)
│   ├── SecurityConfig.java
│   ├── RestClientConfig.java
│   ├── OpenApiConfig.java
│   └── LoggingAspect.java
├── entity/
│   ├── CBDocument.java
│   ├── CBGeneralLog.java
│   └── Enums.java
└── CustomBondApplication.java
```

---

## Summary

> CustomBond implements a **registry-based pluggable pipeline**. Every vendor sends a JSON request with an ordered `steps[]` array. The system looks up each step by name in a pre-built registry and executes them in sequence, passing a shared context object so steps can produce and consume data from prior steps.
>
> Adding a new vendor integration means writing a new step class — no existing code is touched. The system is inherently dynamic: step count, step order, and step selection are all controlled entirely by the vendor at runtime.
