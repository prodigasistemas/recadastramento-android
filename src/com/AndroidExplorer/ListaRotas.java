package com.AndroidExplorer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import ui.FileManager;
import util.Constantes;
import util.Util;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import background.CarregarRotaThread;
import business.Controlador;

public class ListaRotas extends ListActivity {

	private ArrayList<String> item = null;
	private ArrayList<String> path = null;
	private String root;
	private String fileName;
	private CarregarRotaThread progThread;
	private ProgressDialog progDialog;
	MySimpleArrayAdapter fileList;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		this.getListView().setCacheColorHint(Color.TRANSPARENT);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		int[] colors = { 0x12121212, 0xFFFFFFFF, 0x12121212 };
		this.getListView().setDivider(new GradientDrawable(Orientation.RIGHT_LEFT, colors));
		this.getListView().setDividerHeight(1);

		instanciate();
	}

	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
		instanciate();
	}

	public void instanciate() {

		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {

			// We can read and write the media
			File path = Util.getExternalStorageDirectory();
			path.getAbsolutePath();
			Log.i("ExternalStorage", "ExternalStorage :" + path.getAbsolutePath());
			root = path.getAbsolutePath() + Constantes.DIRETORIO_ROTAS;
			getDir(root);

			// Display a messagebox.
			Toast.makeText(getBaseContext(), "Por favor, escolha a rota a ser carregada.", Toast.LENGTH_LONG).show();
		}
	}

	// Handler on the main (UI) thread that will receive messages from the second thread and update the progress.
	final Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			// Get the current value of the variable total from the message data and update the progress bar.
			int total = msg.getData().getInt("total");
			progDialog.setProgress(total);

			if (total >= Controlador.getInstancia().getQtdRegistros() || progThread.getCustomizedState() == CarregarRotaThread.DONE) {
				dismissDialog(Constantes.DIALOG_ID_CARREGAR_ROTA);
				setResult(RESULT_FIRST_USER, new Intent(getBaseContext(), Fachada.class));
				finish();
			}
		}
	};

	private void getDir(String dirPath) {

		item = new ArrayList<String>();
		path = new ArrayList<String>();
		File f = new File(dirPath);
		File[] files = f.listFiles();

		if (files != null) {
			for (int i = 0; i < files.length; i++) {
				File file = files[i];
				path.add(file.getPath());

				if (!file.isDirectory()) {
					if (((file.getName().endsWith(".txt")) || (file.getName().endsWith(".gz"))) && !file.getName().startsWith("._")) {
						item.add(file.getName());
					}
				}
			}
		}
		fileList = new MySimpleArrayAdapter(this, item);
		setListAdapter(fileList);
	}

	@Override
	protected void onListItemClick(ListView l, View view, int position, long id) {

		// user clicked a list item, make it "selected"
		fileList.setSelectedPosition(position);

		if (progThread != null && progThread.getCustomizedState() == CarregarRotaThread.DONE) {
			setResult(RESULT_FIRST_USER, new Intent(getBaseContext(), Fachada.class));
			finish();

		} else {
			fileName = fileList.getListElementName(position);
			carregaRotaDialogButtonClick();

		}
	}

	public void carregaRotaDialogButtonClick() {
		Controlador.getInstancia().initiateDataManipulator(getBaseContext());
		showDialog(Constantes.DIALOG_ID_CARREGAR_ROTA);
	}

	@Override
	protected Dialog onCreateDialog(final int id) {

		if (id == Constantes.DIALOG_ID_CARREGAR_ROTA) {
			progDialog = new ProgressDialog(this);
			progDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progDialog.setMessage("Por favor, espere enquanto a rota está sendo carregada...");
			progDialog.setCancelable(false);
			progDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {

				public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
					if (keyCode == KeyEvent.KEYCODE_SEARCH && event.getRepeatCount() == 0) {
						return true; // Pretend we processed it

					} else if (keyCode == KeyEvent.KEYCODE_HOME && event.getRepeatCount() == 0) {
						return true; // Pretend we processed it
					}
					return false; // Any other keys are still processed as normal
				}
			});

			try {
				int fileLineNumber = FileManager.getFileLineNumber(fileName);

				if (fileLineNumber == Constantes.NULO_INT) {
					showDialog(Constantes.DIALOG_ID_ERRO);
					return null;
				}
				progDialog.setMax(fileLineNumber);

			} catch (IOException e) {
				e.printStackTrace();
			}

			progThread = new CarregarRotaThread(handler, fileName, this);
			progThread.start();
			return progDialog;

		} else if (id == Constantes.DIALOG_ID_ERRO) {

			LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			AlertDialog.Builder builder;

			View layout = inflater.inflate(R.layout.custon_dialog, (ViewGroup) findViewById(R.id.layout_root));
			((TextView) layout.findViewById(R.id.messageDialog)).setText("Arquivo de rota não localizado. Verifique se o cartão de memória está em uso.");

			((ImageView) layout.findViewById(R.id.imageDialog)).setImageResource(R.drawable.aviso);

			builder = new AlertDialog.Builder(this);
			builder.setView(layout);
			builder.setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {

				public void onClick(DialogInterface dialog, int whichButton) {
					removeDialog(id);
				}
			});

			AlertDialog messageDialog = builder.create();
			return messageDialog;
		}

		return null;
	}

	public class MySimpleArrayAdapter extends ArrayAdapter<String> {
		private final Activity context;
		private final ArrayList<String> names;

		// used to keep selected position in ListView
		private int selectedPos = -1;

		public MySimpleArrayAdapter(Activity context, ArrayList<String> names) {
			super(context, R.layout.rowroteiro, names);
			this.context = context;
			this.names = names;
		}

		public void setSelectedPosition(int pos) {
			selectedPos = pos;
			notifyDataSetChanged();
		}

		public int getSelectedPosition() {
			return selectedPos;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater = context.getLayoutInflater();
			View rowView = inflater.inflate(R.layout.rowroteiro, null, true);
			TextView textView = (TextView) rowView.findViewById(R.id.nomerota);
			ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
			textView.setText(names.get(position));

			if (names.get(position).endsWith(".txt")) {
				imageView.setImageResource(R.drawable.text);
			} else {
				imageView.setImageResource(R.drawable.compressed);
			}

			// change the row color based on selected state
			if (selectedPos == position) {
				rowView.setBackgroundColor(Color.argb(70, 255, 255, 255));
			} else {
				rowView.setBackgroundColor(Color.TRANSPARENT);
			}

			return rowView;
		}

		public String getListElementName(int element) {
			return names.get(element);
		}
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			finish();
			return true;

		} else if (keyCode == KeyEvent.KEYCODE_SEARCH) {
			return true;

		} else if (keyCode == KeyEvent.KEYCODE_HOME) {
			return true;

		} else {
			return super.onKeyDown(keyCode, event);
		}
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_importar_banco, menu);

		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.importarBanco:
			Controlador.getInstancia().importDB(ListaRotas.this);
			Intent intent = new Intent(ListaRotas.this, MenuPrincipal.class);
            startActivity(intent);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}