package com.octo.keip.schema.eip.definitions;

import java.util.List;

public sealed interface Restriction {
  record EnumRestriction(List<String> enums) implements Restriction {}
  ;
}
