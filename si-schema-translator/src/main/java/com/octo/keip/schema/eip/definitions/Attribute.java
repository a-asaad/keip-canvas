package com.octo.keip.schema.eip.definitions;

public record Attribute(
    String name,
    String type,
    String description,
    Object defaultValue,
    boolean required,
    Restriction restriction) {

  public Attribute(Builder builder) {
    this(
        builder.name,
        builder.type,
        builder.description,
        builder.defaultValue,
        builder.required,
        builder.restriction);
  }

  public static class Builder {
    private final String name;
    private final String type;
    private String description;
    private Object defaultValue;
    private boolean required;
    private Restriction restriction;

    public Builder(String name, String type) {
      this.name = name;
      this.type = type;
    }

    public Attribute build() {
      return new Attribute(this);
    }

    public Builder description(String description) {
      this.description = description;
      return this;
    }

    public Builder defaultValue(Object defaultValue) {
      this.defaultValue = defaultValue;
      return this;
    }

    public Builder required(boolean required) {
      this.required = required;
      return this;
    }

    public Builder restriction(Restriction restriction) {
      this.restriction = restriction;
      return this;
    }
  }
}
