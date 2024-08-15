package com.task05;

public record Response  (int statusCode, Event event) {
  @Override
  public String toString() {
    return "{" +
            "statusCode=" + statusCode +
            ", event=" + event +
            '}';
  }
}
