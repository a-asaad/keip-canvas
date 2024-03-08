package com.octo.keip.schema.model.eip;

import java.util.Set;

public record ChildGroup(Indicator indicator, Set<EipChildElement> elements) {}
