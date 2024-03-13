package com.octo.keip.schema.model.eip;

// TODO: Handle case when max is unbounded. (e.g. use "unbounded" or -1)
public record Occurrence(int min, int max) {}
