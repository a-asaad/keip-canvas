package com.octo.keip.schema.model.eip;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EipSchema {

  private final Map<String, List<EipComponent>> eipComponentMap = new HashMap<>();

  public void addComponent(String namespace, EipComponent component) {
    List<EipComponent> compList = eipComponentMap.getOrDefault(namespace, new ArrayList<>());
    compList.add(component);
    eipComponentMap.put(namespace, compList);
  }

  public void addComponents(String namespace, List<EipComponent> components) {
    eipComponentMap.put(namespace, components);
  }

  public Map<String, List<EipComponent>> toMap() {
    return eipComponentMap;
  }
}
