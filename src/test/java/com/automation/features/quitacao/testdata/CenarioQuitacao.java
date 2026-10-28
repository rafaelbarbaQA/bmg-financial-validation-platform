package com.automation.features.quitacao.testdata;

public final class CenarioQuitacao {

    private final String nome;
    private final long idConta;
    private final String tipoCorrespondencia;
    private final boolean ativo;

    private CenarioQuitacao(
            Builder builder
    ) {

        validar(
                builder.nome,
                builder.idConta,
                builder.tipoCorrespondencia
        );

        this.nome = builder.nome.trim();
        this.idConta = builder.idConta;
        this.tipoCorrespondencia =
                builder.tipoCorrespondencia.trim();
        this.ativo = builder.ativo;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getNome() {
        return nome;
    }

    public long getIdConta() {
        return idConta;
    }

    public String getTipoCorrespondencia() {
        return tipoCorrespondencia;
    }

    public boolean isAtivo() {
        return ativo;
    }

    private void validar(
            String nome,
            long idConta,
            String tipoCorrespondencia
    ) {

        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException(
                    "O nome do cenário não pode ser vazio."
            );
        }

        if (idConta <= 0) {
            throw new IllegalArgumentException(
                    "O IdConta deve ser maior que zero."
            );
        }

        if (tipoCorrespondencia == null
                || tipoCorrespondencia.isBlank()) {

            throw new IllegalArgumentException(
                    "O TipoCorrespondencia não pode ser vazio."
            );
        }
    }

    @Override
    public String toString() {

        return "CenarioQuitacao{"
                + "nome='"
                + nome
                + '\''
                + ", idConta="
                + idConta
                + ", tipoCorrespondencia='"
                + tipoCorrespondencia
                + '\''
                + ", ativo="
                + ativo
                + '}';
    }

    public static final class Builder {

        private String nome;
        private long idConta;
        private String tipoCorrespondencia;
        private boolean ativo = true;

        private Builder() {
        }

        public Builder comNome(
                String nome
        ) {

            this.nome = nome;
            return this;
        }

        public Builder comIdConta(
                long idConta
        ) {

            this.idConta = idConta;
            return this;
        }

        public Builder comTipoCorrespondencia(
                String tipoCorrespondencia
        ) {

            this.tipoCorrespondencia =
                    tipoCorrespondencia;

            return this;
        }

        public Builder ativo(
                boolean ativo
        ) {

            this.ativo = ativo;
            return this;
        }

        public CenarioQuitacao build() {

            return new CenarioQuitacao(
                    this
            );
        }
    }
}
