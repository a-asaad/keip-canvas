package com.octo.keip.schema.eip.definitions;

import java.util.List;

public record ChildGroup(Indicator indicator, List<EipElement> elements) {}
