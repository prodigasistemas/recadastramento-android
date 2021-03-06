package com.AndroidExplorer;

import java.io.File;
import java.util.Calendar;
import java.util.List;

import model.AnormalidadeImovel;
import model.Imovel;
import util.Constantes;
import util.Util;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.telephony.CellLocation;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import background.EnviarCadastroOnlineThread;
import business.Controlador;

public class AnormalidadeTab extends Fragment implements LocationListener {

	private static final int FOTO_1 = 1;
	private static final int FOTO_2 = 2;
	
	private static boolean existeAnormalidade;

	private Imovel imovel;
	private AnormalidadeImovel anormalidadeImovel;
	
	private LocationManager locationManager;
	private View view;
	private List<String> anormalidades;
	private EditText codigoAnormalidade;
	private Location ultimaLocalizacao;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.anormalidadetab, container, false);

		definirImagemFundo();
		instanciate();

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
	}

	public void onLocationChanged(Location location) {
		((TextView) view.findViewById(R.id.txtValorLatitude)).setText(String.valueOf(location.getLatitude()));
		((TextView) view.findViewById(R.id.txtValorLongitude)).setText(String.valueOf(location.getLongitude()));
	}

	public void onProviderDisabled(String provider) {}

	public void onProviderEnabled(String provider) {}

	public void onStatusChanged(String provider, int status, Bundle extras) {}
	
	public int getCodigoAnormalidade() {
		return Integer.valueOf(((EditText) view.findViewById(R.id.codigoAnormalidade)).getText().toString());
	}
	
	private void definirImagemFundo() {
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			view.setBackgroundResource(R.drawable.fundocadastro);
		} else {
			view.setBackgroundResource(R.drawable.fundocadastro);
		}
	}
	
	private void instanciate() {
		imovel = Controlador.getInstancia().getImovelSelecionado();
		anormalidadeImovel = Controlador.getInstancia().getAnormalidadeImovelSelecionado();
		
		configurarLocalizacaoGeografica();
		obterUltimaLocalizacao();
		configurarCoordenadas();

		configurarListaInconsistencias();
		configurarListaAnormalidades();
		configurarCodigoAnormalidade();

		preencherCampos();
		verificarFotos();

		configurarBotaoAtualizar();
		configurarBotaoFinalizar();
		configurarBotoesFotos();
	}

	private void configurarListaInconsistencias() {
		String[] inconsistencias = Controlador.getInstancia().getCadastroDataManipulator().pesquisarInconsistencias(imovel.getMatricula());
		
		if (inconsistencias.length > 0) {
			String listaFormatada = "";
			for (int i = 0; i < inconsistencias.length; i++) {
				listaFormatada += "- " + inconsistencias[i] + "\n";
			}
			
			((TextView) view.findViewById(R.id.listaInconsistencias)).setText(listaFormatada);
		} else {
			((LinearLayout) view.findViewById(R.id.layoutInconsistencias)).setVisibility(View.GONE);
		}
		
	}

	private void verificarFotos() {
		if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			if (anormalidadeImovel.foto1Vazia() && getFoto("_1.jpg").exists()) {
				((ImageView) view.findViewById(R.id.foto1)).invalidate();
			}

			if (anormalidadeImovel.foto2Vazia() && getFoto("_2.jpg").exists()) {
				((ImageView) view.findViewById(R.id.foto2)).invalidate();
			}
		} else {
			Toast.makeText(getActivity(), "Não foi possível armazenar as imagens.", Toast.LENGTH_LONG).show();
		}
	}
	
	private void preencherCampos() {
		codigoAnormalidade.setText(String.valueOf(anormalidadeImovel.getCodigoAnormalidade()));
		((EditText) view.findViewById(R.id.editComentario)).setText(anormalidadeImovel.getComentario());
	}

	private void configurarBotoesFotos() {
		configurarBotaoFoto(R.id.botaoFoto1, FOTO_1);
		configurarBotaoFoto(R.id.botaoFoto2, FOTO_2);
	}

	private void configurarBotaoFoto(int botaoId, final int fotoId) {
		final Button botao = (Button) view.findViewById(botaoId);
		botao.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				iniciarCamera(fotoId);
			}
		});
	}

	private void configurarCodigoAnormalidade() {
		codigoAnormalidade = (EditText) view.findViewById(R.id.codigoAnormalidade);
		codigoAnormalidade.addTextChangedListener(new TextWatcher() {

			public void beforeTextChanged(CharSequence valor, int start, int count, int after) {}

			public void onTextChanged(CharSequence valor, int start, int before, int after) {

				// Quando o texto é alterado o onTextChange é chamado. Essa flag evita a chamada infinita desse método
				if (existeAnormalidade) {
					existeAnormalidade = false;
					return;
				}

				String descricao = Controlador.getInstancia().getCadastroDataManipulator().selectDescricaoByCodigoFromTable(
						Constantes.TABLE_ANORMALIDADE, valor.toString());
				
				Spinner spinner = (Spinner) (view.findViewById(R.id.spinnerTipoAnormalidade));
				
				if (descricao != null) {
					for (int i = 0; i < anormalidades.size(); i++) {
						if (anormalidades.get(i).equalsIgnoreCase(descricao)) {
							spinner.setSelection(i);
							break;
						} else {
							spinner.setSelection(0);
						}
					}
				}
			}

			public void afterTextChanged(Editable s) {
			}
		});
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void configurarListaAnormalidades() {
		Spinner spinner = (Spinner) view.findViewById(R.id.spinnerTipoAnormalidade);
		anormalidades = Controlador.getInstancia().getCadastroDataManipulator().selectAnormalidades();

		ArrayAdapter<CharSequence> adapter = new ArrayAdapter(getActivity(), android.R.layout.simple_spinner_item, anormalidades);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		spinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			public void onItemSelected(AdapterView parent, View v, int position, long id) {
				String anormalidadeSelecionada = ((Spinner) view.findViewById(R.id.spinnerTipoAnormalidade)).getSelectedItem().toString();
				String codigoAnormalidadeSelecionada = ((EditText) view.findViewById(R.id.codigoAnormalidade)).getText().toString();

				String codigo = Controlador.getInstancia().getCadastroDataManipulator().selectCodigoByDescricaoFromTable(
						Constantes.TABLE_ANORMALIDADE, anormalidadeSelecionada);

				if (codigo.compareTo(codigoAnormalidadeSelecionada) != 0) {
					existeAnormalidade = true;
					codigoAnormalidade.setText(codigo);
				}
			}

			public void onNothingSelected(AdapterView<?> arg0) {}
		});
	}

	private void configurarCoordenadas() {
		TextView campoLatitude = (TextView) view.findViewById(R.id.txtValorLatitude);
		TextView campoLongitude = (TextView) view.findViewById(R.id.txtValorLongitude);

		if (ultimaLocalizacao != null) {
			campoLatitude.setText(String.valueOf(ultimaLocalizacao.getLatitude()));
			campoLongitude.setText(String.valueOf(ultimaLocalizacao.getLongitude()));

		} else if (anormalidadeImovel.coordenadasValida()) {
			campoLatitude.setText(String.valueOf(anormalidadeImovel.getLatitude()));
			campoLongitude.setText(String.valueOf(anormalidadeImovel.getLongitude()));

		} else {
			campoLatitude.setText("----");
			campoLongitude.setText("----");
		}
	}

	private void configurarBotaoAtualizar() {
		final Button botaoAtualizar = (Button) view.findViewById(R.id.botaoAtualizar);
		botaoAtualizar.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				obterUltimaLocalizacao();
				configurarCoordenadas();
			}
		});
	}

	private void obterUltimaLocalizacao() {
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		criteria.setCostAllowed(true);
		String provider = locationManager.getBestProvider(criteria, false);
		ultimaLocalizacao = locationManager.getLastKnownLocation(provider);
		CellLocation.requestLocationUpdate();
	}

	private void configurarLocalizacaoGeografica() {
		locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
		if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
		}

		localizacaoValida();
	}

	private void configurarBotaoFinalizar() {
		final Button botaoFinalizar = (Button) view.findViewById(R.id.botaoFinalizar);
		botaoFinalizar.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				atualizar();

				if (abasFinalizadas()) {
					if (localizacaoValida()) {
						verificarImovel();
						salvar();
						finalizar();
						configurarMensagem();
					}
				} else if (imovel.getImovelStatus() != Constantes.IMOVEL_A_SALVAR) {
					verificarImovel();
					
					Controlador.getInstancia().getCadastroDataManipulator().salvarAnormalidadeImovel();
					Controlador.getInstancia().getCadastroDataManipulator().salvarImovel();
					
					finalizar();
					configurarMensagem();
					
				} else {
					verificarAbasFinalizadas();
				}
			}

			
		});
	}
	
	private void configurarMensagem() {
		String mensagem = "";
		switch (imovel.getImovelStatus()) {

		case Constantes.IMOVEL_SALVO:
			mensagem = "Imóvel finalizado com sucesso.";
			break;

		case Constantes.IMOVEL_SALVO_COM_ANORMALIDADE:
			mensagem = "Imóvel com anormalidade finalizado com sucesso.";
			break;

		case Constantes.IMOVEL_NOVO:
			mensagem = "Imóvel novo criado com sucesso.";
			break;

		case Constantes.IMOVEL_NOVO_COM_ANORMALIDADE:
			mensagem = "Imóvel novo com anormalidade criado com sucesso.";
			break;

		default:
			break;
		}

		showNotifyDialog(R.drawable.save, "", mensagem, Constantes.DIALOG_ID_CONFIRMA_IMOVEL_SALVO);
	}

	private void verificarAbasFinalizadas() {
		String mensagem = null;
		if (!Controlador.getInstancia().getClienteSelecionado().isTabSaved()) {
			mensagem = " Atualize os dados do cliente antes de finalizar. ";
		} else if (!imovel.isTabSaved()) {
			mensagem = " Atualize os dados do imóvel antes de finalizar. ";
		} else if (!Controlador.getInstancia().getServicosSelecionado().isTabSaved()) {
			mensagem = " Atualize os dados de serviço antes de finalizar. ";
		} else if (!Controlador.getInstancia().getMedidorSelecionado().isTabSaved()) {
			mensagem = " Atualize os dados do medidor antes de finalizar. ";
		}

		showNotifyDialog(R.drawable.aviso, "Mensagem:", mensagem, Constantes.DIALOG_ID_ERRO);
	}

	private void finalizar() {
		((MainTab) getActivity()).setTabColor();
		anormalidadeImovel.setTabSaved(true);
		
		EnviarCadastroOnlineThread enviar = new EnviarCadastroOnlineThread(view.getContext(), imovel);
		enviar.start();
	}
	
	private void salvar() {
		imovel.setImovelEnviado(String.valueOf(Constantes.NAO));
		
		Controlador.getInstancia().getCadastroDataManipulator().salvarCliente();
		Controlador.getInstancia().getCadastroDataManipulator().salvarImovel();
		Controlador.getInstancia().getCadastroDataManipulator().salvarServico();
		Controlador.getInstancia().getCadastroDataManipulator().salvarMedidor();
		Controlador.getInstancia().getCadastroDataManipulator().salvarAnormalidadeImovel();
	}
	
	private void verificarImovel() {
		setOperacaoTipo();
		setImovelStatus();
	}

	private void setImovelStatus() {
		int anormalidade = ((Spinner) (view.findViewById(R.id.spinnerTipoAnormalidade))).getSelectedItemPosition();
		
		if (anormalidade == Constantes.SEM_OCORRENCIA) {
			if (!imovel.isImovelNovo()) {
				imovel.setImovelStatus(String.valueOf(Constantes.IMOVEL_SALVO));
			}
		} else {
			if (imovel.isImovelNovo()) {
				imovel.setImovelStatus(String.valueOf(Constantes.IMOVEL_NOVO_COM_ANORMALIDADE));
			} else {
				imovel.setImovelStatus(String.valueOf(Constantes.IMOVEL_SALVO_COM_ANORMALIDADE));
			}
		}
	}

	private void setOperacaoTipo() {
		if (imovel.isImovelNovo()) {
			imovel.setOperacoTipo(Constantes.OPERACAO_CADASTRO_NOVO);
		} else {
			imovel.setOperacoTipo(Constantes.OPERACAO_CADASTRO_ALTERADO);
		}
	}

	private void iniciarCamera(int fotoId) {
		Log.d("CAMERA", "Iniciando câmera...");
		Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
		intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);

		if (fotoId == FOTO_1) {
			if (getFoto("_1.jpg").exists()) {
				getFoto("_1.jpg").delete();
			}

			intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(getFoto("_1.jpg")));

		} else {
			if (getFoto("_2.jpg").exists()) {
				getFoto("_2.jpg").delete();
			}
			
			intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(getFoto("_2.jpg")));
		}

		startActivityForResult(intent, fotoId);
	}

	private File getFoto(String sufixo) {
		final File path = new File(Util.getRetornoRotaDirectory());
		if (!path.exists()) {
			path.mkdir();
		}
		
		return new File(path, imovel.getMatricula() + sufixo);
	}

	private void atualizar() {
		anormalidadeImovel.setMatricula(imovel.getMatricula());

		if (codigoAnormalidade.getText().toString().length() > 0) {
			anormalidadeImovel.setCodigoAnormalidade(Integer.parseInt(codigoAnormalidade.getText().toString()));
		} else {
			anormalidadeImovel.setCodigoAnormalidade(0);
		}

		anormalidadeImovel.setComentario(((EditText) view.findViewById(R.id.editComentario)).getText().toString());

		if (getFoto("_1.jpg").exists()) {
			anormalidadeImovel.setFoto1(imovel.getMatricula() + "_1.jpg");
		}

		if (getFoto("_2.jpg").exists()) {
			anormalidadeImovel.setFoto2(imovel.getMatricula() + "_2.jpg");
		}

		String latitude = ((TextView) view.findViewById(R.id.txtValorLatitude)).getText().toString();
		if (!latitude.equalsIgnoreCase("----")) {
			anormalidadeImovel.setLatitude(latitude);
		}

		String longitude = ((TextView) view.findViewById(R.id.txtValorLongitude)).getText().toString();
		if (!longitude.equalsIgnoreCase("----")) {
			anormalidadeImovel.setLongitude(longitude);
		}

		anormalidadeImovel.setData(Util.formatarData(Calendar.getInstance().getTime()));
		anormalidadeImovel.setLoginUsuario(Controlador.getInstancia().getCadastroDataManipulator().getUsuario().getLogin());
	}

	private boolean abasFinalizadas() {
		int anormalidade = ((Spinner) (view.findViewById(R.id.spinnerTipoAnormalidade))).getSelectedItemPosition();

		if (anormalidade == Constantes.SEM_OCORRENCIA) {
			if (!imovel.isTabSaved() || 
				!Controlador.getInstancia().getClienteSelecionado().isTabSaved() || 
				!Controlador.getInstancia().getServicosSelecionado().isTabSaved() || 
				!Controlador.getInstancia().getMedidorSelecionado().isTabSaved()) {

				return false;
			}
		}
		
		return true;
	}

	private boolean localizacaoValida() {
		boolean habilitado = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

		if (!habilitado) {
			showNotifyDialog(R.drawable.aviso, "Alerta!", "GPS está desligado. Por favor, ligue-o para finalizar o cadastro.",
					Constantes.DIALOG_ID_ERRO_GPS_DESLIGADO);
		}

		return habilitado;
	}

	private void showNotifyDialog(int iconId, String title, String message, int messageType) {
		NotifyAlertDialogFragment newFragment = NotifyAlertDialogFragment.newInstance(iconId, title, message, messageType);
		newFragment.setTargetFragment(this, Constantes.FRAGMENT_ID_ANORMALIDADE);
		newFragment.show(getActivity().getSupportFragmentManager(), "dialog");
	}
}