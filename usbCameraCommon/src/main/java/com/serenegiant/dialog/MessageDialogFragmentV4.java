package com.serenegiant.dialog;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2018 saki t_saki@serenegiant.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;

import android.util.Log;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

public class MessageDialogFragmentV4 extends DialogFragment {
    //	private static final boolean DEBUG = false;	// FIXME 実働時はfalseにすること
    private static final String TAG = MessageDialogFragmentV4.class.getSimpleName();

    public static interface MessageDialogListener {
        public void onMessageDialogResult(final MessageDialogFragmentV4 dialog, final int requestCode, final String[] permissions, final boolean result);
    }

    public static MessageDialogFragmentV4 showDialog(final FragmentActivity parent, final int requestCode, final int id_title, final int id_message, final String[] permissions) {
        final MessageDialogFragmentV4 dialog = newInstance(requestCode, id_title, id_message, permissions);
        dialog.show(parent.getSupportFragmentManager(), TAG);
        return dialog;
    }

    public static MessageDialogFragmentV4 showDialog(final Fragment parent, final int requestCode, final int id_title, final int id_message, final String[] permissions) {
        final MessageDialogFragmentV4 dialog = newInstance(requestCode, id_title, id_message, permissions);
        dialog.setTargetFragment(parent, parent.getId());
        dialog.show(parent.getFragmentManager(), TAG);
        return dialog;
    }

    public static MessageDialogFragmentV4 newInstance(final int requestCode, final int id_title, final int id_message, final String[] permissions) {
        final MessageDialogFragmentV4 fragment = new MessageDialogFragmentV4();
        final Bundle args = new Bundle();
        // ここでパラメータをセットする
        args.putInt("requestCode", requestCode);
        args.putInt("title", id_title);
        args.putInt("message", id_message);
        args.putStringArray("permissions", permissions != null ? permissions : new String[]{});
        fragment.setArguments(args);
        return fragment;
    }

    private MessageDialogListener mDialogListener;

    public MessageDialogFragmentV4() {
        super();
        // デフォルトコンストラクタが必要
    }

    @SuppressLint("NewApi")
    @Override
    public void onAttach(final Activity context) {
        super.onAttach(context);
        // コールバックインターフェースを取得
        if (context instanceof MessageDialogListener) {
            mDialogListener = (MessageDialogListener) context;
        }
        if (mDialogListener == null) {
            final Fragment fragment = getTargetFragment();
            if (fragment instanceof MessageDialogListener) {
                mDialogListener = (MessageDialogListener) fragment;
            }
        }
        if (mDialogListener == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                final Fragment target = getParentFragment();
                if (target instanceof MessageDialogListener) {
                    mDialogListener = (MessageDialogListener) target;
                }
            }
        }
        if (mDialogListener == null) {
//			Log.w(TAG, "caller activity/fragment must implement PermissionDetailDialogFragmentListener");
            throw new ClassCastException(context.toString());
        }
    }

//	@Override
//	public void onCreate(final Bundle savedInstanceState) {
//		super.onCreate(savedInstanceState);
//		final Bundle args = savedInstanceState != null ? savedInstanceState : getArguments();
//	}

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        final Bundle args = savedInstanceState != null ? savedInstanceState : getArguments();
        final int requestCode = getArguments().getInt("requestCode");
        final int id_title = getArguments().getInt("title");
        final int id_message = getArguments().getInt("message");
        final String[] permissions = args.getStringArray("permissions");


        return new AlertDialog.Builder(getActivity())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(id_title)
                .setMessage(id_message)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialog, final int whichButton) {
                                // 本当はここでパーミッション要求をしたいだけどこのダイアログがdismissしてしまって結果を受け取れないので
                                // 呼び出し側へ返してそこでパーミッション要求する。なのでこのダイアログは単にメッセージを表示するだけ
                                try {
                                    mDialogListener.onMessageDialogResult(MessageDialogFragmentV4.this, requestCode, permissions, true);
                                } catch (final Exception e) {
                                    Log.w(TAG, e);
                                }
                            }
                        }
                )
                .setNegativeButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialog, int whichButton) {
                                try {
                                    mDialogListener.onMessageDialogResult(MessageDialogFragmentV4.this, requestCode, permissions, false);
                                } catch (final Exception e) {
                                    Log.w(TAG, e);
                                }
                            }
                        }
                )
                .create();
    }

}
