package com.automation.core.xml;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.io.StringReader;
import java.util.Objects;

public final class XmlHelper {

    private XmlHelper() {
        // Impede a criação de objetos desta classe utilitária.
    }

    public static Document parsear(String xml) {

        if (xml == null || xml.isBlank()) {
            throw new IllegalArgumentException(
                    "O XML não pode ser nulo ou vazio."
            );
        }

        try {
            DocumentBuilderFactory factory =
                    criarFactorySegura();

            DocumentBuilder builder =
                    factory.newDocumentBuilder();

            return builder.parse(
                    new InputSource(
                            new StringReader(xml)
                    )
            );

        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "Não foi possível interpretar o XML.",
                    exception
            );
        }
    }

    public static String obterValorPorNomeLocal(
            String xml,
            String nomeElemento
    ) {

        Document document = parsear(xml);

        return obterValorPorNomeLocal(
                document,
                nomeElemento
        );
    }

    public static String obterValorPorNomeLocal(
            Document document,
            String nomeElemento
    ) {

        Objects.requireNonNull(
                document,
                "O documento XML não pode ser nulo."
        );

        validarNomeElemento(nomeElemento);

        NodeList elementos =
                document.getElementsByTagNameNS(
                        "*",
                        nomeElemento
                );

        if (elementos.getLength() == 0) {
            throw new IllegalStateException(
                    "Elemento não encontrado no XML: "
                            + nomeElemento
            );
        }

        return elementos
                .item(0)
                .getTextContent()
                .trim();
    }

    public static boolean possuiElementoPorNomeLocal(
            Document document,
            String nomeElemento
    ) {

        Objects.requireNonNull(
                document,
                "O documento XML não pode ser nulo."
        );

        validarNomeElemento(nomeElemento);

        return document
                .getElementsByTagNameNS(
                        "*",
                        nomeElemento
                )
                .getLength() > 0;
    }

    private static void validarNomeElemento(
            String nomeElemento
    ) {

        if (nomeElemento == null
                || nomeElemento.isBlank()) {

            throw new IllegalArgumentException(
                    "O nome do elemento XML não pode ser vazio."
            );
        }
    }

    private static DocumentBuilderFactory criarFactorySegura()
            throws ParserConfigurationException {

        DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();

        factory.setNamespaceAware(true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        factory.setFeature(
                XMLConstants.FEATURE_SECURE_PROCESSING,
                true
        );

        factory.setFeature(
                "http://apache.org/xml/features/disallow-doctype-decl",
                true
        );

        factory.setFeature(
                "http://xml.org/sax/features/external-general-entities",
                false
        );

        factory.setFeature(
                "http://xml.org/sax/features/external-parameter-entities",
                false
        );

        factory.setFeature(
                "http://apache.org/xml/features/nonvalidating/load-external-dtd",
                false
        );

        factory.setAttribute(
                XMLConstants.ACCESS_EXTERNAL_DTD,
                ""
        );

        factory.setAttribute(
                XMLConstants.ACCESS_EXTERNAL_SCHEMA,
                ""
        );

        return factory;
    }
}
