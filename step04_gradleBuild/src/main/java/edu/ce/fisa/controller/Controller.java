package edu.ce.fisa.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Controller {

	@GetMapping("/get")
	public String getFn() {
		return "Get 방식 테스트입니당 :) 업데이트 테스트!!! 2";
	}
	
	@PostMapping("/post")
	public String postFn() {
		return "POST 방식 테스트입니당 :)";
	}
}
