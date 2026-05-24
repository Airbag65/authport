package com.port.auth.types;

public class SmtpLoginReq {
 private String email;
 private String password;

 public SmtpLoginReq(String email, String password) {
  this.email = email;
  this.password = password;
 }

 public String getEmail() {
  return this.email;
 }

 public String getPassword() {
  return this.password;
 }
}
