package com.restaurant.soporte;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Doble de prueba (stub) para el tiempo. Permite probar tiempos de atención
 * sin Thread.sleep: la prueba decide cuánto "pasa" entre un estado y otro,
 * así es rápida y siempre da el mismo resultado.
 */
public class RelojFalso extends Clock {

    private Instant ahora;

    public RelojFalso(LocalDateTime inicio) {
        this.ahora = inicio.toInstant(ZoneOffset.UTC);
    }

    public static RelojFalso el(int anio, int mes, int dia, int hora, int minuto) {
        return new RelojFalso(LocalDateTime.of(anio, mes, dia, hora, minuto));
    }

    public void avanzar(Duration d) {
        ahora = ahora.plus(d);
    }

    public void avanzarMinutos(long minutos) {
        avanzar(Duration.ofMinutes(minutos));
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return ahora;
    }
}
