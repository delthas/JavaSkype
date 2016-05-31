#include <jni.h>
#include "skyloginconnector.h"
#include "skylogin.h"

JNIEXPORT jstring JNICALL Java_fr_delthas_skype_SkyLoginConnector_getUIC (JNIEnv *env, jclass obj, jstring username, jstring password, jstring nonce) {
	const char *cusername = (*env)->GetStringUTFChars(env, username, NULL);
	if (NULL == cusername) return NULL;
	const char *cpassword = (*env)->GetStringUTFChars(env, password, NULL);
	if (NULL == cpassword) return NULL;
	const char *cnonce = (*env)->GetStringUTFChars(env, nonce, NULL);
	if (NULL == cnonce) return NULL;
	SkyLogin login = SkyLogin_Init();
	SkyLogin_PerformLogin(login, (char *)cusername, (char *)cpassword);
	char uic[UICSTR_SIZE];
	int len = SkyLogin_CreateUICString(login, cnonce, uic);
	uic[len] = 0;
	SkyLogin_Exit(login);
	(*env)->ReleaseStringUTFChars(env, username, cusername);
	(*env)->ReleaseStringUTFChars(env, password, cpassword);
	(*env)->ReleaseStringUTFChars(env, nonce, cnonce);
	return (*env)->NewStringUTF(env, uic);
}
