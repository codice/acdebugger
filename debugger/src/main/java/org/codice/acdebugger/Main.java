package org.codice.acdebugger;

import picocli.CommandLine;

public class Main {
  public static void main(String[] args) throws Exception {
    CommandLine.call(new ACDebugger(), args);
  }
}
