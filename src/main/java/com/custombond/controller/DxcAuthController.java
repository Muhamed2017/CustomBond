package com.custombond.controller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;


import com.custombond.auth.DXC_Auth;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/dxc/auth")
public class DxcAuthController {

    @Autowired
    DXC_Auth DXC_auth;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> LoginReq) {
        return DXC_auth.Login(LoginReq.get("UserName"), LoginReq.get("Password"));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(@RequestBody Map<String, String> LogoutReq) {
        return DXC_auth.Logout(LogoutReq.get("token"));
    }


}
