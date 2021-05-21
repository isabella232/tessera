package com.quorum.tessera.api.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.quorum.tessera.api.MockVersion;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.json.Json;
import org.junit.Before;
import org.junit.Test;

public class VersionResourceTest {

  private VersionResource instance;

  @Before
  public void onSetUp() {
    this.instance = new VersionResource();
  }

  @Test
  public void getVersion() {
    assertThat(instance.getVersion()).isEqualTo(MockVersion.VERSION);
  }

  @Test
  public void getDistributionVersion() {
    assertThat(instance.getDistributionVersion()).isEqualTo(MockVersion.VERSION);
  }

  @Test
  public void getVersions() {
    assertThat(instance.getVersions())
        .containsExactlyElementsOf(
            Stream.of("1.0", "2.0", "2.1", "3.0")
                .map(Json::createValue)
                .collect(Collectors.toSet()));
  }
}
