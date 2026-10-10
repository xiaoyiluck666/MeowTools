package com.xiaoyiluck.meowtools.module;

public interface Module {
    void enable();
    void disable();
    void reload();
    String getName();
}
