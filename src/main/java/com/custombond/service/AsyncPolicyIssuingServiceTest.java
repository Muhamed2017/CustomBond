package com.custombond.service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.custombond.dto.request.BlackListRequest;
import com.custombond.dto.request.CB_IssuePolicyRequest;

@Service
public class AsyncPolicyIssuingServiceTest {

    @Autowired
    DXC_BlackList_Service dxc_blackList_service;

@Async("externalApiExecutor")
public CompletableFuture<String> AsyncFullCycle_CB(CB_IssuePolicyRequest c)
 { 
    
System.out.println(dxc_blackList_service.checkBlackList(BlackListRequest.toEntity(c)));
    // System.out.println("Async process started at: " + LocalDateTime.now());

    // try {
    //     Thread.sleep(180000); // 3 minutes = 180000 ms
    // } catch (InterruptedException e) {
    //     Thread.currentThread().interrupt();
    // }

    // System.out.println("TRYING TO CALL EXTERNAL API...");

    String result = "Async process completed at: " + LocalDateTime.now();

    return CompletableFuture.completedFuture(result);
}

}
