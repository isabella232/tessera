package com.quorum.tessera.p2p.recovery;

import com.quorum.tessera.p2p.resend.ResendClient;

public interface RecoveryClient extends ResendClient {

  boolean pushBatch(String targetUrl, PushBatchRequest request);

  ResendBatchResponse makeBatchResendRequest(String targetUrl, ResendBatchRequest request);
}
