package xapi.jre.model;

/** The original committed operation and store revision, returned again for identical retries. */
public final class ModelTransactionReceipt {
  private final ModelTransaction transaction;
  private final String fingerprint;
  private final long revision;

  ModelTransactionReceipt(final ModelTransaction transaction, final String fingerprint, final long revision) {
    this.transaction = transaction;
    this.fingerprint = fingerprint;
    this.revision = revision;
  }

  public ModelTransaction transaction() { return transaction; }
  public String fingerprint() { return fingerprint; }
  public long revision() { return revision; }
}
