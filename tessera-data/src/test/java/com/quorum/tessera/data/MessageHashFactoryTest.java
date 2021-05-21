package com.quorum.tessera.data;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class MessageHashFactoryTest {

  @Test
  public void createFromCipherText() {
    MessageHashFactory messageHashFactory = new MessageHashFactory() {};
    String cipherText = "cipherText";
    MessageHash messageHash = messageHashFactory.createFromCipherText(cipherText.getBytes());

    assertThat(messageHash).isNotNull();
  }

  @Test
  public void create() {
    assertThat(MessageHashFactory.create()).isNotNull();
  }
}
