package ch.uzh.csg.mbps.client.request;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;

import ch.uzh.csg.mbps.client.IAsyncTaskCompleteListener;
import ch.uzh.csg.mbps.client.servercomm.CookieHandler;
import ch.uzh.csg.mbps.client.servercomm.CustomRestTemplate;
import ch.uzh.csg.mbps.client.util.Constants;
import ch.uzh.csg.mbps.keys.CustomPublicKey;
import ch.uzh.csg.mbps.responseobject.CustomResponseObject;

//TODO jeton: javadoc
public class CommitPublicKeyRequestTask extends RequestTask {
	
	private CustomPublicKey cpk;
	
	public CommitPublicKeyRequestTask(IAsyncTaskCompleteListener<CustomResponseObject> cro, CustomPublicKey cpk) {
		this.callback = cro;
		this.cpk = cpk;
		this.url = Constants.BASE_URI_SSL + "/user/savePublicKey";
	}

	@Override
	protected CustomResponseObject responseService(CustomRestTemplate restTemplate) {
		@SuppressWarnings("rawtypes")
		HttpEntity requestEntity = CookieHandler.getAuthHeaderCustomPublicKey(cpk);
		return restTemplate.exchange(url, HttpMethod.POST, requestEntity);
	}

}
