package com.octo.keip.schema.model.eip;

import java.util.Objects;

/** A top-level EIP component: Message Endpoint or Channel. */
public final class EipComponent extends EipElement {

  private final Role role;

  // TODO: Is flowType needed for channels? Does it make more sense to split Endpoints and Channels
  // into different subclasses.
  private final FlowType flowType;

  private EipComponent(Builder builder) {
    super(builder);
    this.role = builder.role;
    this.flowType = builder.flowType;
  }

  public Role getRole() {
    return role;
  }

  public FlowType getFlowType() {
    return flowType;
  }

  public static class Builder extends EipElement.Builder<Builder> {

    private final Role role;
    private final FlowType flowType;

    public Builder(String name, Role role, FlowType flowType) {
      this.name = Objects.requireNonNull(name);
      this.role = Objects.requireNonNull(role);
      this.flowType = Objects.requireNonNull(flowType);
    }

    @Override
    public EipComponent build() {
      return new EipComponent(this);
    }

    @Override
    protected Builder self() {
      return this;
    }
  }
}
