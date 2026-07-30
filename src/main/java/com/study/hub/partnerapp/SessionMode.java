package com.study.hub.partnerapp;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Способ создания партнёрской сессии")
public enum SessionMode {
    TOKEN,
    CODE
}
