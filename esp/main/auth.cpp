#include "auth.hpp"
#include "config.hpp"
#include "tekla/b64.hpp"
#include "nvs.h"
#include "esp_random.h"
#include "esp_timer.h"
#include "mbedtls/pkcs5.h"
#include "mbedtls/sha256.h"
#include "tekla/netcfg.hpp"
#include <cstring>
namespace auth {
static Session S[cfg::kMaxSessions];
struct Fail { uint32_t ip; int n; int64_t t; }; static Fail F[8];
static bool hash(const char* pw,const uint8_t* salt,uint8_t* out){return mbedtls_pkcs5_pbkdf2_hmac_ext(MBEDTLS_MD_SHA256,(const unsigned char*)pw,strlen(pw),salt,16,cfg::kPbkdf2Iter,32,out)==0;}
static bool ct_eq(const uint8_t*a,const uint8_t*b,size_t n){uint8_t d=0;for(size_t i=0;i<n;i++)d|=a[i]^b[i];return d==0;}
static bool store(nvs_handle_t h,const char* key,const char* secret){if(!secret||strlen(secret)<8)return false;uint8_t blob[48];esp_fill_random(blob,16);if(!hash(secret,blob,blob+16))return false;return nvs_set_blob(h,key,blob,48)==ESP_OK;}
static bool check(nvs_handle_t h,const char* key,const char* secret){if(!secret)return false;uint8_t blob[48],hv[32];size_t n=48;if(nvs_get_blob(h,key,blob,&n)!=ESP_OK||n!=48)return false;return hash(secret,blob,hv)&&ct_eq(hv,blob+16,32);}
bool configured(){nvs_handle_t h;if(nvs_open("auth",NVS_READONLY,&h)!=ESP_OK)return false;size_t n=0;bool ok=nvs_get_blob(h,"pw",nullptr,&n)==ESP_OK&&n==48;nvs_close(h);return ok;}
bool setup(const char* pw){if(configured()||!pw||strlen(pw)<8)return false;nvs_handle_t h;if(nvs_open("auth",NVS_READWRITE,&h)!=ESP_OK)return false;bool ok=store(h,"pw",pw)&&nvs_commit(h)==ESP_OK;nvs_close(h);return ok;}
static int verify_rl(const char* key,const char* pw,uint32_t ip){int64_t now=esp_timer_get_time();Fail*f=nullptr;for(auto&x:F)if(x.ip==ip)f=&x;if(!f){f=&F[0];for(auto&x:F)if(x.t<f->t)f=&x;*f={ip,0,0};}if(f->n>=cfg::kLoginMaxFails&&now-f->t<(int64_t)cfg::kLoginLockMs*1000)return 2;nvs_handle_t h;if(nvs_open("auth",NVS_READONLY,&h)!=ESP_OK)return 1;bool ok=check(h,key,pw);nvs_close(h);if(!ok){f->n++;f->t=now;return 1;}f->n=0;return 0;}
int verify_limited(const char*pw,uint32_t key){return verify_rl("pw",pw,key);}
static Session* slot(){Session*s=nullptr;for(auto&x:S)if(!x.used){s=&x;break;}if(!s){s=&S[0];for(auto&x:S)if(x.last_us<s->last_us)s=&x;}return s;}
int login(const char*pw,uint32_t ip,char*tok){int rc=verify_rl("pw",pw,ip);if(rc)return rc;int64_t now=esp_timer_get_time();Session*s=slot();uint8_t r[24];esp_fill_random(r,sizeof r);*s={};s->used=true;tekla::b64url(r,sizeof r,s->token,sizeof s->token);s->last_us=now;strcpy(tok,s->token);return 0;}
struct Client{uint8_t hash[32];char name[24];};static Client CL[8];static int g_ncl=-1;
static void cl_load(){if(g_ncl>=0)return;g_ncl=0;nvs_handle_t h;if(nvs_open("auth",NVS_READONLY,&h)==ESP_OK){size_t n=sizeof CL;if(nvs_get_blob(h,"cl",CL,&n)==ESP_OK)g_ncl=(int)(n/sizeof(Client));nvs_close(h);}if(g_ncl>8)g_ncl=0;}
static bool cl_save(){nvs_handle_t h;if(nvs_open("auth",NVS_READWRITE,&h)!=ESP_OK)return false;bool ok=nvs_set_blob(h,"cl",CL,g_ncl*sizeof(Client))==ESP_OK&&nvs_commit(h)==ESP_OK;nvs_close(h);return ok;}
static void tok_hash(const char*t,uint8_t out[32]){mbedtls_sha256((const unsigned char*)t,strlen(t),out,0);}
Session* find(const char*token){if(!token||!*token)return nullptr;int64_t now=esp_timer_get_time();for(auto&s:S)if(s.used&&!strcmp(s.token,token)){if(s.client<0&&now-s.last_us>(int64_t)cfg::kSessionTtlMs*1000){s.used=false;return nullptr;}s.last_us=now;return&s;}if(strlen(token)==43){cl_load();uint8_t h[32];tok_hash(token,h);for(int i=0;i<g_ncl;i++)if(ct_eq(h,CL[i].hash,32)){Session*s=slot();*s={};s->used=true;strlcpy(s->token,token,sizeof s->token);s->client=(int8_t)i;s->last_us=now;return s;}}return nullptr;}
int pair(const char*pw,const char*name,uint32_t ip,char*tok){if(!tekla::name_ok(name))return 4;char tmp[48];int rc=login(pw,ip,tmp);if(rc)return rc;if(Session*t=find(tmp))t->used=false;cl_load();if(g_ncl>=8)return 3;uint8_t r[32];esp_fill_random(r,sizeof r);char b[48];if(!tekla::b64url(r,sizeof r,b,sizeof b))return 3;Client&c=CL[g_ncl];tok_hash(b,c.hash);memset(c.name,0,sizeof c.name);strlcpy(c.name,name,sizeof c.name);g_ncl++;if(!cl_save()){g_ncl--;return 3;}strcpy(tok,b);return 0;}
int client_count(){cl_load();return g_ncl;}const char*client_name(int i){cl_load();return i>=0&&i<g_ncl?CL[i].name:"";}
bool client_revoke(int i){cl_load();if(i<0||i>=g_ncl)return false;for(int k=i;k+1<g_ncl;k++)CL[k]=CL[k+1];g_ncl--;bool ok=cl_save();for(auto&s:S)if(s.client>=0)s.used=false;return ok;}
void client_revoke_all(){cl_load();g_ncl=0;cl_save();for(auto&s:S)if(s.client>=0)s.used=false;}
int service_unlock(Session*s,const char*pw,uint32_t ip){char tmp[48];int rc=login(pw,ip,tmp);if(rc)return rc;if(Session*t=find(tmp))t->used=false;s->svc_us=esp_timer_get_time();return 0;}
bool service_ok(Session*s){return s&&s->svc_us&&esp_timer_get_time()-s->svc_us<5LL*60*1000000;}
bool recovery_configured(){nvs_handle_t h;if(nvs_open("auth",NVS_READONLY,&h)!=ESP_OK)return false;size_t n=0;bool ok=nvs_get_blob(h,"rec",nullptr,&n)==ESP_OK&&n==48;nvs_close(h);return ok;}
bool recovery_set(const char*code){if(!code||strlen(code)<16||strlen(code)>64)return false;nvs_handle_t h;if(nvs_open("auth",NVS_READWRITE,&h)!=ESP_OK)return false;bool ok=store(h,"rec",code)&&nvs_commit(h)==ESP_OK;nvs_close(h);return ok;}
int recover(const char*code,const char*np,uint32_t key){if(!recovery_configured())return 4;if(!np||strlen(np)<8)return 3;int rc=verify_rl("rec",code?code:"",key);if(rc)return rc;return set_password(np)?0:3;}
bool verify_password(const char*pw){nvs_handle_t h;if(nvs_open("auth",NVS_READONLY,&h)!=ESP_OK)return false;bool ok=check(h,"pw",pw);nvs_close(h);return ok;}
bool set_password(const char*pw){if(!pw||strlen(pw)<8)return false;nvs_handle_t h;if(nvs_open("auth",NVS_READWRITE,&h)!=ESP_OK)return false;bool ok=store(h,"pw",pw)&&nvs_commit(h)==ESP_OK;nvs_close(h);for(auto&s:S)if(s.client<0)s.used=false;return ok;}
void factory_reset(){nvs_handle_t h;if(nvs_open("auth",NVS_READWRITE,&h)==ESP_OK){nvs_erase_all(h);nvs_commit(h);nvs_close(h);}if(nvs_open("wifi",NVS_READWRITE,&h)==ESP_OK){nvs_erase_all(h);nvs_commit(h);nvs_close(h);}if(nvs_open("ip",NVS_READWRITE,&h)==ESP_OK){nvs_erase_all(h);nvs_commit(h);nvs_close(h);}if(nvs_open("dev",NVS_READWRITE,&h)==ESP_OK){nvs_erase_all(h);nvs_commit(h);nvs_close(h);}g_ncl=0;for(auto&x:S)x.used=false;}
bool dev_configured(){nvs_handle_t h;if(nvs_open("auth",NVS_READONLY,&h)!=ESP_OK)return false;size_t n=0;bool ok=nvs_get_blob(h,"dev",nullptr,&n)==ESP_OK&&n==48;nvs_close(h);return ok;}
bool dev_set(const char*pw){if(!pw||strlen(pw)<8)return false;nvs_handle_t h;if(nvs_open("auth",NVS_READWRITE,&h)!=ESP_OK)return false;bool ok=store(h,"dev",pw)&&nvs_commit(h)==ESP_OK;nvs_close(h);return ok;}
int dev_unlock(Session*s,const char*pw,uint32_t ip){int rc=verify_rl("dev",pw,ip);if(rc)return rc;s->dev_us=esp_timer_get_time();return 0;}
bool dev_ok(Session*s){return s&&s->dev_us&&esp_timer_get_time()-s->dev_us<5LL*60*1000000;}
}
