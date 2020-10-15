package com.cmlteam.file_storage_checker;

import java.util.ArrayList;
import java.util.List;

public class Errors {

  List<String> errorMsgs = new ArrayList<>();

  void addError(String msg, Resp resp) {
    errorMsgs.add(msg + ". Response was: \n--------" + resp + "\n---------\n");
  }

  public String report() {
    return "Errors found:\n\n" + String.join("\n", errorMsgs);
  }
}
