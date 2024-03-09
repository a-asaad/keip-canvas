package com.octo.keip.schema.model.eip;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EipSchema {

  private final Map<String, List<EipComponent>> eipComponentMap;

  public EipSchema() {
    this.eipComponentMap = new HashMap<>();
  }

  private EipSchema(Map<String, List<EipComponent>> eipComponentMap) {
    this.eipComponentMap = eipComponentMap;
  }

  public static EipSchema from(Map<String, List<EipComponent>> componentMap) {
    return new EipSchema(componentMap);
  }

  public Map<String, List<EipComponent>> toMap() {
    return eipComponentMap;
  }

  public void addComponent(String namespace, EipComponent component) {
    List<EipComponent> compList = eipComponentMap.getOrDefault(namespace, new ArrayList<>());
    compList.add(component);
    eipComponentMap.put(namespace, compList);
  }

  public void addComponents(String namespace, List<EipComponent> components) {
    eipComponentMap.put(namespace, components);
  }
}
