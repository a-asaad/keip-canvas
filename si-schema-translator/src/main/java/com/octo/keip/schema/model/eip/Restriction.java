package com.octo.keip.schema.model.eip;

import java.util.List;

// TODO: This is not exhaustive. XML schemas support many more restriction types.
// Decide which ones we should support.
public sealed interface Restriction {
  record EnumRestriction(List<String> enums) implements Restriction {}
  ;
}
