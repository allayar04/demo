package com.task05;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class Event {
  private String id;
  private int principalId;
  private String createdAt;
  private Map<String, Object> body;

  public Event(int principalId, Map<String, Object> body) {
    this.principalId = principalId;
    this.body = body;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public int getPrincipalId() {
    return principalId;
  }

  public void setPrincipalId(int principalId) {
    this.principalId = principalId;
  }

  public String getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(String createdAt) {
    this.createdAt = createdAt;
  }

  public Map<String, Object> getBody() {
    return body;
  }

  public void setBody(Map<String, Object> body) {
    this.body = body;
  }

  @Override
  public String toString() {
    return "{" +
        "id='" + id + '\'' +
        ", principalId=" + principalId +
        ", createdAt='" + createdAt + '\'' +
        ", body=" + body +
        '}';
  }
}
