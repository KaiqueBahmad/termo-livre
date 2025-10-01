package kaiquebt.dev.termolivre.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

class TermoFilterTest {

    private TermoFilter termoFilter;

    @BeforeEach
    void setUp() {
        termoFilter = new TermoFilter();
    }

    @Test
    void testIsMessageSafe_WithNullMessage_ShouldReturnTrue() {
        assertTrue(termoFilter.isMessageSafe(null));
    }

    @Test
    void testIsMessageSafe_WithEmptyMessage_ShouldReturnTrue() {
        assertTrue(termoFilter.isMessageSafe(""));
    }

    @Test
    void testIsMessageSafe_WithBlankMessage_ShouldReturnTrue() {
        assertTrue(termoFilter.isMessageSafe("   "));
    }

    @Test
    void testIsMessageSafe_WithSafeMessage_ShouldReturnTrue() {
        assertTrue(termoFilter.isMessageSafe("Esta é uma mensagem normal"));
        assertTrue(termoFilter.isMessageSafe("Olá, como você está?"));
        assertTrue(termoFilter.isMessageSafe("teste de mensagem segura"));
    }

    @Test
    void testIsMessageSafe_WithDirectAnswer_ShouldReturnFalse() {
        assertFalse(termoFilter.isMessageSafe("casa"));
        assertFalse(termoFilter.isMessageSafe("A palavra é casa"));
        assertFalse(termoFilter.isMessageSafe("casa é bonita"));
    }

    @Test
    void testIsMessageSafe_WithAnswerInUpperCase_ShouldReturnFalse() {
        assertFalse(termoFilter.isMessageSafe("CASA"));
        assertFalse(termoFilter.isMessageSafe("Porta"));
    }

    @Test
    void testIsMessageSafe_WithAnswerWithAccents_ShouldReturnFalse() {
        assertFalse(termoFilter.isMessageSafe("cásá"));
        assertFalse(termoFilter.isMessageSafe("pôrta"));
    }

    @Test
    void testIsMessageSafe_WithSpacedLetters_ShouldReturnFalse() {
        assertFalse(termoFilter.isMessageSafe("c a s a"));
        assertFalse(termoFilter.isMessageSafe("p o r t a"));
    }

    @Test
    void testIsMessageSafe_WithReversedWord_ShouldReturnFalse() {
        assertFalse(termoFilter.isMessageSafe("asac"));
        assertFalse(termoFilter.isMessageSafe("atrop"));
    }

    @Test
    void testIsMessageSafe_WithoutVowels_ShouldReturnFalse() {
        // Note: "casa" sem vogais é "cs", que tem 2 caracteres
        // "porta" sem vogais é "prt", que tem 3 caracteres
        assertFalse(termoFilter.isMessageSafe("prt"));
        // "livro" sem vogais é "lvr", que tem 3 caracteres
        assertFalse(termoFilter.isMessageSafe("lvr"));
    }

    @Test
    void testIsMessageSafe_WithWordBoundaries_ShouldWorkCorrectly() {
        // "casa" está dentro de "casamento" - deve ser seguro se não for a palavra exata
        // Mas como usamos \\b para verificar limites de palavra, deve ser seguro
        assertTrue(termoFilter.isMessageSafe("casamento"));
        // "porta" está em "importante" - deve ser seguro
        assertTrue(termoFilter.isMessageSafe("importante"));
    }
}
