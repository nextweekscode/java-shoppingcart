package com.lambdaschool.shoppingcart.controllers;

import com.lambdaschool.shoppingcart.models.User;
import com.lambdaschool.shoppingcart.models.UserMinimum;
import com.lambdaschool.shoppingcart.models.UserRoles;
import com.lambdaschool.shoppingcart.services.RoleService;
import com.lambdaschool.shoppingcart.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
public class OpenController {

    @Autowired
    UserService userService;

    @Autowired
    RoleService roleService;

    // http://localhost:2019/createnewuser
    // body => username, password, primary email

    @PostMapping(value = "/createnewuser", consumes = {"application/json"}, produces = {"application/json"})
    public ResponseEntity<?> addNewUser(HttpServletRequest httpServletRequest, @RequestBody UserMinimum newinuser) {

        // Create new user
        User newUser = new User();
        newUser.setUsername(newinuser.getUsername());
        newUser.setPassword(newinuser.getPassword());
//        newUser.setPrimaryemail(newinuser.getPrimaryemail());

        Set<UserRoles> newRoles = new HashSet<>();
        newRoles.add(new UserRoles(newUser, roleService.findByName("USER")));

        newUser = userService.save(newUser);
        HttpHeaders responseHeaders = new HttpHeaders();
        // http://localhost:2019/users/user/id
        URI newUserURI =
                ServletUriComponentsBuilder.fromUriString(httpServletRequest.getServerName() + ":" + httpServletRequest.getLocalPort() + "/users/user/{userid}")
                        .buildAndExpand(newUser.getUserid()).toUri();
        responseHeaders.setLocation(newUserURI);

        // Login & return access token
        RestTemplate restTemplate = new RestTemplate();
        String requestURI = "http://" + httpServletRequest.getServerName() + (httpServletRequest.getServerName().equalsIgnoreCase("localhost") ? ":" + httpServletRequest.getLocalPort() : "") + "/login";
        List<MediaType> acceptableMediaTypes = new ArrayList<>();
        acceptableMediaTypes.add(MediaType.APPLICATION_JSON);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(acceptableMediaTypes);
        headers.setBasicAuth(System.getenv("OAUTHCLIENTID"),
                System.getenv("OAUTHCLIENTSECRET"));

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("grant_type",
                "password");
        map.add("scope",
                "read write trust");
        map.add("username",
                newUser.getUsername());
        map.add("password",
                newUser.getPassword());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        String theToken = restTemplate.postForObject(requestURI, request, String.class);

        return new ResponseEntity<>(theToken, responseHeaders, HttpStatus.CREATED);
    }
}
