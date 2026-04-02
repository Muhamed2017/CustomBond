package com.custombond.dto.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DXCQuotePreparationApiErrorResponse {

    private String type;
    private String title;
    private Integer status;
    private Map<String, List<String>> errors;
}
