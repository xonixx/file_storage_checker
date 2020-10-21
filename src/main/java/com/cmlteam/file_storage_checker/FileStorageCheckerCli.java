package com.cmlteam.file_storage_checker;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;

@RequiredArgsConstructor
public class FileStorageCheckerCli implements CommandLineRunner {
  private final Req req;

  @Override
  public void run(String... args) throws Exception {
    new FileStorageChecker(req, args[0], args[1], Boolean.getBoolean("tagsAsFile"), 0).run();
  }
}
