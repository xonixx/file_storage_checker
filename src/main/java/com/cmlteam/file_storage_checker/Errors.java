package com.cmlteam.file_storage_checker;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Errors {

  private final List<String> errorMsgs = new ArrayList<>();
  private final Set<String> uniqueErrors = new HashSet<>();
  private int i = 0;

  void addError(String msg, Resp resp) {
    if (!uniqueErrors.contains(msg)) {
      uniqueErrors.add(msg);
      errorMsgs.add((++i) + ". " + msg + ":\n\n" + resp + "\n");
    }
  }

  public String report() {
    return "FOUND " + errorMsgs.size() + " ERRORS:\n\n" + String.join("\n", errorMsgs);
  }
}
