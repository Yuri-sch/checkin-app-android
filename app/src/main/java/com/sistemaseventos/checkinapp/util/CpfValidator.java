package com.sistemaseventos.checkinapp.util;

public class CpfValidator {

    public static boolean isValid(String cpf) {
        // Remove caracteres não numéricos
        cpf = cpf.replaceAll("[^0-9]", "");

        // Verifica tamanho e se todos os dígitos são iguais (ex: 111.111.111-11)
        if (cpf.length() != 11 || isAllDigitsEqual(cpf)) {
            return false;
        }

        try {
            // Calculo do 1º Dígito Verificador
            int sum = 0;
            int weight = 10;
            for (int i = 0; i < 9; i++) {
                sum += (cpf.charAt(i) - '0') * weight--;
            }

            int r = 11 - (sum % 11);
            char dig10 = (r == 10 || r == 11) ? '0' : (char) (r + '0');

            // Calculo do 2º Dígito Verificador
            sum = 0;
            weight = 11;
            for (int i = 0; i < 10; i++) {
                sum += (cpf.charAt(i) - '0') * weight--;
            }

            r = 11 - (sum % 11);
            char dig11 = (r == 10 || r == 11) ? '0' : (char) (r + '0');

            // Verifica se os dígitos calculados conferem com os informados
            return (dig10 == cpf.charAt(9)) && (dig11 == cpf.charAt(10));
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isAllDigitsEqual(String cpf) {
        char first = cpf.charAt(0);
        for (int i = 1; i < cpf.length(); i++) {
            if (cpf.charAt(i) != first) return false;
        }
        return true;
    }
}
