package com.automation.features.quitacao.validator;

import com.automation.features.quitacao.soap.response
        .GerarBoletoQuitacaoAntecipadaResponse;

import org.testng.annotations.Test;

public class GerarBoletoQuitacaoAntecipadaValidatorTest {

    private final GerarBoletoQuitacaoAntecipadaValidator validator =
            new GerarBoletoQuitacaoAntecipadaValidator();

    @Test
    public void deveAceitarBoletoNaoRegistradoSemMensagemExterna() {

        GerarBoletoQuitacaoAntecipadaResponse response =
                criarResposta(
                        false,
                        ""
                );

        validator.validar(response);
    }

    @Test
    public void deveAceitarBoletoRegistradoExternamente() {

        GerarBoletoQuitacaoAntecipadaResponse response =
                criarResposta(
                        true,
                        "Boleto registrado com sucesso."
                );

        validator.validar(response);
    }

    private GerarBoletoQuitacaoAntecipadaResponse criarResposta(
            boolean registrado,
            String mensagemRegistro
    ) {

        String xml =
                "<GerarBoletoQuitacaoAntecipadaResponse>"
                        + "<IdBoleto>457854999</IdBoleto>"
                        + "<CodRetorno>0</CodRetorno>"
                        + "<DescricaoRetorno>Ok</DescricaoRetorno>"
                        + "<IDLog>LOG-TESTE-001</IDLog>"
                        + "<FlagRegistrado>"
                        + registrado
                        + "</FlagRegistrado>"
                        + "<RetornoMensagemRegistro>"
                        + mensagemRegistro
                        + "</RetornoMensagemRegistro>"
                        + "</GerarBoletoQuitacaoAntecipadaResponse>";

        return new GerarBoletoQuitacaoAntecipadaResponse(
                xml
        );
    }
}
