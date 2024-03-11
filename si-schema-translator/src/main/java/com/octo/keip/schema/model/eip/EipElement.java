package com.octo.keip.schema.model.eip;

import java.util.HashSet;
import java.util.Set;

public abstract class EipElement {

  protected final String name;
  protected final String description;
  protected final Set<Attribute> attributes;
  protected final ChildGroup childGroup;

  protected EipElement(Builder<?> builder) {
    this.name = builder.name;
    this.description = builder.description;
    this.attributes = builder.attributes;
    this.childGroup = builder.childGroup;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public Set<Attribute> getAttributes() {
    return attributes;
  }

  public ChildGroup getChildGroup() {
    return childGroup;
  }

  // Effective Java - Hierarchical builder pattern
  protected abstract static class Builder<T extends Builder<T>> {

    protected String name;
    protected String description;
    protected Set<Attribute> attributes;
    protected ChildGroup childGroup;

    public T description(String description) {
      this.description = description;
      return self();
    }

    public T attributes(Set<Attribute> attributes) {
      this.attributes = attributes;
      return self();
    }

    public T addAttribute(Attribute attribute) {
      if (this.attributes == null) {
        this.attributes = new HashSet<>();
      }
      this.attributes.add(attribute);
      return self();
    }

    public T childGroup(ChildGroup childGroup) {
      this.childGroup = childGroup;
      return self();
    }

    protected abstract EipElement build();

    protected abstract T self();
  }
}
