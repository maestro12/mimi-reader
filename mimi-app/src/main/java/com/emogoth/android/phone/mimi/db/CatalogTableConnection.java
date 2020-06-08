package com.emogoth.android.phone.mimi.db;


import com.emogoth.android.phone.mimi.db.model.CatalogPostModel;
import com.mimireader.chanlib.models.ChanCatalog;
import com.mimireader.chanlib.models.ChanPost;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Function;

public class CatalogTableConnection {
    public static final String LOG_TAG = CatalogTableConnection.class.getSimpleName();

    public static Single<List<CatalogPostModel>> fetchPosts() {
        return DatabaseUtils.fetchTable(CatalogPostModel.class, CatalogPostModel.TABLE_NAME);
    }

    public static Function<List<CatalogPostModel>, List<ChanPost>> convertDbPostsToChanPosts() {
        return CatalogPostModels -> {
            if (CatalogPostModels == null || CatalogPostModels.size() == 0) {
                return Collections.emptyList();
            }

            List<ChanPost> posts = new ArrayList<>(CatalogPostModels.size());
            for (CatalogPostModel dbPost : CatalogPostModels) {
                posts.add(dbPost.toPost());
            }

            return posts;
        };
    }

    public static Observable<Boolean> putPosts(ChanCatalog catalog) {
        if (catalog == null) {
            return Observable.just(false);
        }

        List<ChanPost> posts = catalog.getPosts();

        if (posts == null) {
            return Observable.just(false);
        }

        List<CatalogPostModel> catalogPosts = new ArrayList<>(posts.size());
        for (int i = 0; i < posts.size(); i++) {
            catalogPosts.add(new CatalogPostModel(posts.get(i)));
        }

        return DatabaseUtils.insert(catalogPosts).first(false).toObservable();
    }

    public static Single<Boolean> removeThread(long threadId) {
        CatalogPostModel model = new CatalogPostModel();
        DatabaseUtils.WhereArg[] args = new DatabaseUtils.WhereArg[1];

        args[0] = new DatabaseUtils.WhereArg(CatalogPostModel.KEY_POST_ID + "=?", threadId);
        return DatabaseUtils.remove(model, true, args).single(false);
    }

    public static Single<Boolean> clear() {
        CatalogPostModel model = new CatalogPostModel();
        DatabaseUtils.WhereArg[] args = new DatabaseUtils.WhereArg[0];
        return DatabaseUtils.remove(model, true, args).single(false);
    }
}
