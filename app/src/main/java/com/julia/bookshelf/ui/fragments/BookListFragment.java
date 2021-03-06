package com.julia.bookshelf.ui.fragments;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.julia.bookshelf.R;
import com.julia.bookshelf.model.dao.BookDAO;
import com.julia.bookshelf.model.data.Book;
import com.julia.bookshelf.model.database.BookshelfDatabaseHelper;
import com.julia.bookshelf.model.database.DatabaseManager;
import com.julia.bookshelf.model.database.tasks.LoadBooksFromDatabaseTask;
import com.julia.bookshelf.model.http.InternetAccess;
import com.julia.bookshelf.model.http.URLCreator;
import com.julia.bookshelf.model.tasks.LoadBooksTask;
import com.julia.bookshelf.ui.adapters.BookAdapter;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class BookListFragment extends BaseFragment {
    public interface OnListItemClickedListener {
        public void onListItemClicked(Book book);
    }

    private BookAdapter rvAdapter;

    public static Fragment newInstance() {
        return new BookListFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.book_list, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        initView(view);

        DatabaseManager.initializeInstance(new BookshelfDatabaseHelper(getContext()));
        loadBooksFromDatabase();

        if (InternetAccess.isInternetConnection(getActivity().getApplicationContext())) {
            loadBooksFromServer();
        } else {
            InternetAccess.showNoInternetConnection(getActivity().getApplicationContext());
        }
    }

    private void loadBooksFromServer() {
        LoadBooksTask loadBooksTask = new LoadBooksTask(URLCreator.loadBook()) {
            @Override
            protected void onPostExecute(final List<Book> books) {
                updateView(books);
                saveBookListInDatabase(books);
            }
        };
        loadBooksTask.execute();
    }

    private void saveBookListInDatabase(final List<Book> books) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                BookDAO bookDAO = new BookDAO();
                bookDAO.deleteAllBooks();
                bookDAO.addBookList(books);
            }
        }).start();
    }

    private void loadBooksFromDatabase() {
        LoadBooksFromDatabaseTask loadBooksFromDatabaseTask = new LoadBooksFromDatabaseTask() {
            @Override
            protected void onPostExecute(List<Book> books) {
                if (books != null) {
                    updateView(books);
                }
            }
        };
        loadBooksFromDatabaseTask.execute();
    }

    private void updateView(@NonNull List<Book> bookList) {
        Collections.sort(bookList, new Comparator<Book>() {
            @Override
            public int compare(Book book1, Book book2) {
                return book1.getTitle().compareTo(book2.getTitle());
            }
        });
        rvAdapter.updateData(bookList);
        rvAdapter.notifyDataSetChanged();
    }

    private void initView(View view) {
        RecyclerView rvBookList = (RecyclerView) view.findViewById(R.id.rv_book_list);
        rvBookList.setHasFixedSize(true);
        Context context = getActivity().getApplicationContext();
        RecyclerView.LayoutManager rvManager = new GridLayoutManager(context, 3);
        rvBookList.setLayoutManager(rvManager);
        rvAdapter = new BookAdapter(context);
        rvAdapter.setOnItemClickListener(new BookAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View v, int position) {
                getListener().onListItemClicked(rvAdapter.getBook(position));
            }
        });
        rvBookList.setAdapter(rvAdapter);
    }

    private OnListItemClickedListener getListener() {
        return (OnListItemClickedListener) getActivity();
    }
}
