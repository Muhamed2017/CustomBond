package com.custombond.auth;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


@Service
// Class For DXC Authentication Endpoints like Login/Logout/GetToken
public class DXC_Auth {
    @Value("${app.external.dxc-url}")
    String BasicURL;
    @Value("${app.external.username}")
    String DXC_UserName;
    @Value("${app.external.password}")
    String DXC_Password;
    @Value("${app.external.login-url}")
    private String LoginURL;
    @Value("${app.external.logout-url}")
    private String LogoutURL;

    @Autowired
    private RestTemplate restTemplate;

    public ResponseEntity<Map<String, Object>> Login(String UserName, String Password) {

        Map<String, Object> request = new HashMap<>();
        request.put("loginName", UserName);
        request.put("password", Password);
        System.out.println("ptr:" + request.toString());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
        return restTemplate.exchange(
                LoginURL,
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {
                });
    }

    public ResponseEntity<Map<String, Object>> Logout(String Token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(Token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<Map<String, Object>> DXC_Response = restTemplate.exchange(
                LogoutURL,
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {
                });
        return DXC_Response;

    }

    public String getDxcToken() {
        ResponseEntity<Map<String, Object>> LoginInfo = Login(DXC_UserName, DXC_Password);
        String Token = Optional.ofNullable(LoginInfo.getBody())
                .map(b -> b.get("accessToken"))
                .map(Object::toString)
                .orElseThrow(() -> new RuntimeException("Access token missing"));
        return Token;
    }

}
