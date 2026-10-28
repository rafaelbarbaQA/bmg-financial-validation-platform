package com.automation.features.quitacao.model;

public final class BoletoEmitido {

    private final long idConta;
    private final long idBoleto;

    public BoletoEmitido(
            long idConta,
            long idBoleto
    ) {

        if (idConta <= 0) {
            throw new IllegalArgumentException(
                    "O IdConta deve ser maior que zero."
            );
        }

        if (idBoleto <= 0) {
            throw new IllegalArgumentException(
                    "O IdBoleto deve ser maior que zero."
            );
        }

        this.idConta = idConta;
        this.idBoleto = idBoleto;
    }

    public long getIdConta() {
        return idConta;
    }

    public long getIdBoleto() {
        return idBoleto;
    }

    @Override
    public String toString() {

        return "BoletoEmitido{"
                + "idConta="
                + idConta
                + ", idBoleto="
                + idBoleto
                + '}';
    }
}
