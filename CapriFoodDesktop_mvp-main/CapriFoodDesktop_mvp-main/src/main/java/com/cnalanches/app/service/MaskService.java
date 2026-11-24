package com.cnalanches.app.service;

import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class MaskService {

    private static final int MAX_DIGITOS = 12;
    private final DecimalFormatSymbols simbolos = new DecimalFormatSymbols(new Locale("pt", "BR"));

    /** Aplica a máscara de Real (R$ 1.234,56) no TextField. */
    public void aplicarMascaraMoeda(TextField campo) {
        campo.setTextFormatter(new TextFormatter<String>(change -> {
            if (!change.isContentChange()) {
                return change;
            }

            String digitos = change.getControlNewText().replaceAll("\\D", "");
            if (digitos.length() > MAX_DIGITOS) {
                digitos = digitos.substring(0, MAX_DIGITOS);
            }

            String formatado = "";
            if (!digitos.isEmpty()) {
                BigInteger centavos = new BigInteger(digitos);
                if (centavos.signum() != 0) {
                    formatado = formatarMoeda(new BigDecimal(centavos, 2));
                }
            }

            change.setRange(0, change.getControlText().length());
            change.setText(formatado);
            change.selectRange(formatado.length(), formatado.length());
            return change;
        }));
    }

    /** Converte o texto mascarado em BigDecimal. Vazio vira 0.00. */
    public BigDecimal moedaParaBigDecimal(String texto) {
        if (texto == null) return BigDecimal.ZERO.setScale(2);
        String digitos = texto.replaceAll("\\D", "");
        if (digitos.isEmpty()) return BigDecimal.ZERO.setScale(2);
        return new BigDecimal(new BigInteger(digitos), 2);
    }

    /** Formata um valor como "R$ 1.234,56". */
    public String formatarMoeda(BigDecimal valor) {
        DecimalFormat df = new DecimalFormat("#,##0.00", simbolos);
        return "R$ " + df.format(valor);
    }
}