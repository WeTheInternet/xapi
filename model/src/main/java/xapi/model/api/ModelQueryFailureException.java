package xapi.model.api;

/**
 * QueryFailureException:
 * <p>
 * <p>
 * Created by James X. Nelson (James@WeTheInter.net) on 31/01/2024 @ 3:40 a.m.
 */
public class ModelQueryFailureException extends Exception {
    private final ModelQuery<? extends Model> query;
    private final String filePath;

    public ModelQueryFailureException(final ModelQuery<? extends Model> query, final String filePath, final Throwable t) {
        super("Failure on query " + query, t);
        this.query = query;
        this.filePath = filePath;
    }

    public ModelQuery<? extends Model> getQuery() {
        return query;
    }

    public String getFilePath() {
        return filePath;
    }
}
