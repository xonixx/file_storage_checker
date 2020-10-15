package com.cmlteam.file_storage_checker;

import java.util.ArrayList;
import java.util.List;

public class Errors {

  private final List<String> errorMsgs = new ArrayList<>();
  private int i = 0;

  void addError(String msg, Resp resp) {
    String line = "\n------------\n";
    errorMsgs.add((++i) + ". " + msg + ". Response was: " + line + resp + line);
  }

  public String report() {
    return "FOUND " + errorMsgs.size() + " ERRORS:\n\n" + String.join("\n", errorMsgs);
  }
}