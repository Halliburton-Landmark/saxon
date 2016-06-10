#include "SchemaValidator.h"
#include "XdmNode.h"
#include "XdmValue.h"
#include "XdmItem.h"

//#define DEBUG

SchemaValidator::SchemaValidator() {
	SaxonProcessor *p = new SaxonProcessor(true);
	SchemaValidator(p, "");
}

SchemaValidator::SchemaValidator(SaxonProcessor* p, std::string curr){
	proc = p;
	/*
	 * Look for class.
	 */
	cppClass = lookForClass(sxn_environ->env,
			"net/sf/saxon/option/cpp/SchemaValidatorForCpp");
	if ((proc->proc) == NULL) {
		std::cerr << "Processor is NULL" << std::endl;
	}

	cppV = createSaxonProcessor2(sxn_environ->env, cppClass,
			"(Lnet/sf/saxon/s9api/Processor;)V", proc->proc);

#ifdef DEBUG
	jmethodID debugMID = sxn_environ->env->GetStaticMethodID(cppClass, "setDebugMode", "(Z)V");
	sxn_environ->env->CallStaticVoidMethod(cppClass, debugMID, (jboolean)true);
#endif    

	proc->exception = NULL;
	if(!(proc->cwd.empty()) && curr.empty()){
		cwdV = proc->cwd;
	} else {
		cwdV = curr;
	}
	if(exceptionOccurred()) {
				if(proc->exception != NULL) {
					delete proc->exception;
				}
				proc->exception = proc->checkForExceptionCPP(sxn_environ->env, cppClass, NULL);
				
     			
			
#ifdef DEBUG
		sxn_environ->env->ExceptionDescribe();
#endif
		proc->exceptionClear();
	}

}

   void SchemaValidator::setcwd(const char* dir){
	if(dir==NULL) {
    		cwdV = std::string(dir);
	}
   }

  void SchemaValidator::setOutputFile(const char * sourceFile){
	outputFile = std::string(sourceFile);
  }

  XdmNode * SchemaValidator::getValidationReport(){
	jmethodID mID =
		(jmethodID) sxn_environ->env->GetMethodID(cppClass, "getValidationReport", "()Lnet/sf/saxon/s9api/XdmNode;");
	if (!mID) {
		std::cerr << "Error: libsaxon." << "validate.getValidationReport()" << " not found\n"
			<< std::endl;
	} else {
		jobject result = (jobject)(
			sxn_environ->env->CallObjectMethod(cppV, mID));
		
		if (result) {
			XdmNode * node = new XdmNode(result);
			node->setProcessor(proc);
			return node;
		}
	}
	return NULL;
}

  void SchemaValidator::registerSchemaFromFile(const char * sourceFile){
	if (sourceFile == NULL) {
		std::cerr << "Error:: sourceFile string cannot be empty or NULL" << std::endl;
	     return;
        }
	
	jmethodID mID =
		(jmethodID) sxn_environ->env->GetMethodID(cppClass, "registerSchema",
				"(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/Object;)V");
	if (!mID) {
		std::cerr << "Error: libsaxon." << "validate" << " not found\n"
			<< std::endl;
	} else {
	jobjectArray stringArray = NULL;
	jobjectArray objectArray = NULL;
	jclass objectClass = lookForClass(sxn_environ->env, "java/lang/Object");
	jclass stringClass = lookForClass(sxn_environ->env, "java/lang/String");

	int size = parameters.size() + properties.size();
#ifdef DEBUG
		std::cerr<<"Properties size: "<<properties.size()<<std::endl;
		std::cerr<<"Parameter size: "<<parameters.size()<<std::endl;
		std::cerr<<"size:"<<size<<std::endl;
#endif
	if (size > 0) {
		objectArray = sxn_environ->env->NewObjectArray((jint) size,
				objectClass, 0);
		stringArray = sxn_environ->env->NewObjectArray((jint) size,
				stringClass, 0);
		int i = 0;
		for (std::map<std::string, XdmValue*>::iterator iter = parameters.begin();
				iter != parameters.end(); ++iter, i++) {
			sxn_environ->env->SetObjectArrayElement(stringArray, i,
					sxn_environ->env->NewStringUTF((iter->first).c_str()));
			sxn_environ->env->SetObjectArrayElement(objectArray, i,
					(iter->second)->getUnderlyingValue(proc));
#ifdef DEBUG
				std::string s1 = typeid(iter->second).name();
				std::cerr<<"Type of itr:"<<s1<<std::endl;
				jobject xx = (iter->second)->getUnderlyingValue(proc);
				if(xx == NULL) {
					std::cerr<<"value failed"<<std::endl;
				} else {

					std::cerr<<"Type of value:"<<(typeid(xx).name())<<std::endl;
				}
				if((iter->second)->getUnderlyingValue(proc) == NULL) {
					std::cerr<<"(iter->second)->getUnderlyingValue() is NULL"<<std::endl;
				}
#endif
		}
		for (std::map<std::string, std::string>::iterator iter = properties.begin();
				iter != properties.end(); ++iter, i++) {
			sxn_environ->env->SetObjectArrayElement(stringArray, i,
					sxn_environ->env->NewStringUTF((iter->first).c_str()));
			sxn_environ->env->SetObjectArrayElement(objectArray, i,
					sxn_environ->env->NewStringUTF((iter->second).c_str()));
		}
	}
	
			sxn_environ->env->CallVoidMethod(cppV, mID,
					sxn_environ->env->NewStringUTF(cwdV.c_str()),
					sxn_environ->env->NewStringUTF(sourceFile), stringArray, objectArray);

	if (size > 0) {
		sxn_environ->env->DeleteLocalRef(stringArray);
		sxn_environ->env->DeleteLocalRef(objectArray);
	}

}
		if(exceptionOccurred()) {
				if(proc->exception != NULL) {
					delete proc->exception;
				}
				proc->exception = proc->checkForExceptionCPP(sxn_environ->env, cppClass, NULL);
				
     			
			
#ifdef DEBUG
		sxn_environ->env->ExceptionDescribe();
#endif
		proc->exceptionClear();
	}
 }

  void SchemaValidator::registerSchemaFromString(const char * sourceStr){
	setProperty("resources", proc->getResourcesDirectory());
	
	if (sourceStr == NULL) {
		std::cerr << "Error:: Schema string cannot be empty or NULL" << std::endl;
	     return;
        }
	jmethodID mID =
		(jmethodID) sxn_environ->env->GetMethodID(cppClass, "registerSchemaString",
				"(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/Object;)V");

	if (!mID) {
		std::cerr << "Error: libsaxon." << "registerSchemaString" << " not found\n"
			<< std::endl;
	} else {
	jobjectArray stringArray = NULL;
	jobjectArray objectArray = NULL;
	jclass objectClass = lookForClass(sxn_environ->env, "java/lang/Object");
	jclass stringClass = lookForClass(sxn_environ->env, "java/lang/String");

	int size = parameters.size() + properties.size();
#ifdef DEBUG
		std::cerr<<"Properties size: "<<properties.size()<<std::endl;
		std::cerr<<"Parameter size: "<<parameters.size()<<std::endl;
		std::cerr<<"size:"<<size<<std::endl;
#endif

	if (size > 0) {
		objectArray = sxn_environ->env->NewObjectArray((jint) size,
				objectClass, 0);
		stringArray = sxn_environ->env->NewObjectArray((jint) size,
				stringClass, 0);
		int i = 0;
		for (std::map<std::string, XdmValue*>::iterator iter = parameters.begin();
				iter != parameters.end(); ++iter, i++) {
			sxn_environ->env->SetObjectArrayElement(stringArray, i,
					sxn_environ->env->NewStringUTF((iter->first).c_str()));
			sxn_environ->env->SetObjectArrayElement(objectArray, i,
					(iter->second)->getUnderlyingValue(proc));
#ifdef DEBUG
				std::string s1 = typeid(iter->second).name();
				std::cerr<<"Type of itr:"<<s1<<std::endl;
				jobject xx = (iter->second)->getUnderlyingValue(proc);
				if(xx == NULL) {
					std::cerr<<"value failed"<<std::endl;
				} else {

					std::cerr<<"Type of value:"<<(typeid(xx).name())<<std::endl;
				}
				if((iter->second)->getUnderlyingValue(proc) == NULL) {
					std::cerr<<"(iter->second)->getUnderlyingValue() is NULL"<<std::endl;
				}
#endif
		}
		for (std::map<std::string, std::string>::iterator iter = properties.begin();
				iter != properties.end(); ++iter, i++) {
			sxn_environ->env->SetObjectArrayElement(stringArray, i,
					sxn_environ->env->NewStringUTF((iter->first).c_str()));
			sxn_environ->env->SetObjectArrayElement(objectArray, i,
					sxn_environ->env->NewStringUTF((iter->second).c_str()));
		}
	}

	
			sxn_environ->env->CallVoidMethod(cppV, mID,
					sxn_environ->env->NewStringUTF(cwdV.c_str()),
					(sourceStr != NULL ? sxn_environ->env->NewStringUTF(sourceStr) : NULL), NULL, stringArray, objectArray);

	if (size > 0) {
		sxn_environ->env->DeleteLocalRef(stringArray);
		sxn_environ->env->DeleteLocalRef(objectArray);
	}

}

		if(exceptionOccurred()) {

				if(proc->exception != NULL) {
					delete proc->exception;
				}
				proc->exception = proc->checkForExceptionCPP(sxn_environ->env, cppClass, NULL);
				
     			
#ifdef DEBUG
			sxn_environ->env->ExceptionDescribe();
#endif
			proc->exceptionClear();
		}

	
}

  void SchemaValidator::validate(const char * sourceFile){
	/*if (sourceFile == NULL) {
		std::cerr << "Error:: sourceFile string cannot be empty or NULL" << std::endl;
	     return;
        }*/
setProperty("resources", proc->getResourcesDirectory());
jmethodID mID =
		(jmethodID) sxn_environ->env->GetMethodID(cppClass, "validate",
				"(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/Object;)V");
if (!mID) {
	std::cerr << "Error: libsaxon." << "validate" << " not found\n"
			<< std::endl;

} else {
	jobjectArray stringArray = NULL;
	jobjectArray objectArray = NULL;
	jclass objectClass = lookForClass(sxn_environ->env, "java/lang/Object");
	jclass stringClass = lookForClass(sxn_environ->env, "java/lang/String");

	int size = parameters.size() + properties.size();
#ifdef DEBUG
		std::cerr<<"Properties size: "<<properties.size()<<std::endl;
		std::cerr<<"Parameter size: "<<parameters.size()<<std::endl;
		std::cerr<<"size:"<<size<<std::endl;
#endif
	if (size > 0) {
		objectArray = sxn_environ->env->NewObjectArray((jint) size,
				objectClass, 0);
		stringArray = sxn_environ->env->NewObjectArray((jint) size,
				stringClass, 0);
		int i = 0;
		for (std::map<std::string, XdmValue*>::iterator iter = parameters.begin();
				iter != parameters.end(); ++iter, i++) {
			sxn_environ->env->SetObjectArrayElement(stringArray, i,
					sxn_environ->env->NewStringUTF((iter->first).c_str()));
			sxn_environ->env->SetObjectArrayElement(objectArray, i,
					(iter->second)->getUnderlyingValue(proc));

#ifdef DEBUG
				std::string s1 = typeid(iter->second).name();
				std::cerr<<"param-name:"<<(iter->first)<<",  "<<"Type of itr:"<<s1<<std::endl;
				jobject xx = (iter->second)->getUnderlyingValue(proc);
				if(xx == NULL) {
					std::cerr<<"value failed"<<std::endl;
				} else {

					std::cerr<<"Type of value:"<<(typeid(xx).name())<<std::endl;
				}
				if((iter->second)->getUnderlyingValue(proc) == NULL) {
					std::cerr<<"(iter->second)->getUnderlyingValue() is NULL"<<std::endl;
				}
#endif
		}
		for (std::map<std::string, std::string>::iterator iter = properties.begin();
				iter != properties.end(); ++iter, i++) {
			sxn_environ->env->SetObjectArrayElement(stringArray, i,
					sxn_environ->env->NewStringUTF((iter->first).c_str()));
			sxn_environ->env->SetObjectArrayElement(objectArray, i,
					sxn_environ->env->NewStringUTF((iter->second).c_str()));

		}
	}
	
			sxn_environ->env->CallVoidMethod(cppV, mID,
					sxn_environ->env->NewStringUTF(cwdV.c_str()), 
					(sourceFile != NULL ? sxn_environ->env->NewStringUTF(sourceFile) : NULL), (outputFile.empty() ? NULL : outputFile.c_str() ), stringArray, objectArray);

	if (size > 0) {
		sxn_environ->env->DeleteLocalRef(stringArray);
		sxn_environ->env->DeleteLocalRef(objectArray);
	}
		if(exceptionOccurred()) {
			if(proc->exception != NULL) {
				delete proc->exception;
			}
				proc->exception = proc->checkForExceptionCPP(sxn_environ->env, cppClass, NULL);
				
     			


#ifdef DEBUG
	sxn_environ->env->ExceptionDescribe();
#endif
			proc->exceptionClear();
		}
		}	
}
   
XdmNode * SchemaValidator::validateToNode(const char * sourceFile){
	if (sourceFile == NULL) {
		std::cerr << "Error:: source file string cannot be empty or NULL" << std::endl;
	     return NULL;
        }
setProperty("resources", proc->getResourcesDirectory());
jmethodID mID =
		(jmethodID) sxn_environ->env->GetMethodID(cppClass, "validateToNode",
				"(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/Object;)Lnet/sf/saxon/s9api/XdmNode;");
if (!mID) {
	std::cerr << "Error: libsaxon." << "validate" << " not found\n"
			<< std::endl;

} else {
	jobjectArray stringArray = NULL;
	jobjectArray objectArray = NULL;
	jclass objectClass = lookForClass(sxn_environ->env, "java/lang/Object");
	jclass stringClass = lookForClass(sxn_environ->env, "java/lang/String");

	int size = parameters.size() + properties.size();
#ifdef DEBUG
		std::cerr<<"Properties size: "<<properties.size()<<std::endl;
		std::cerr<<"Parameter size: "<<parameters.size()<<std::endl;
		std::cerr<<"size:"<<size<<std::endl;
#endif
	if (size > 0) {
		objectArray = sxn_environ->env->NewObjectArray((jint) size,
				objectClass, 0);
		stringArray = sxn_environ->env->NewObjectArray((jint) size,
				stringClass, 0);
		int i = 0;
		for (std::map<std::string, XdmValue*>::iterator iter = parameters.begin();
				iter != parameters.end(); ++iter, i++) {
			sxn_environ->env->SetObjectArrayElement(stringArray, i,
					sxn_environ->env->NewStringUTF((iter->first).c_str()));
			sxn_environ->env->SetObjectArrayElement(objectArray, i,
					(iter->second)->getUnderlyingValue(proc));
#ifdef DEBUG
				std::string s1 = typeid(iter->second).name();
				std::cerr<<"Type of itr:"<<s1<<std::endl;
				jobject xx = (iter->second)->getUnderlyingValue(proc);
				if(xx == NULL) {
					std::cerr<<"value failed"<<std::endl;
				} else {

					std::cerr<<"Type of value:"<<(typeid(xx).name())<<std::endl;
				}
				if((iter->second)->getUnderlyingValue(proc) == NULL) {
					std::cerr<<"(iter->second)->getUnderlyingValue() is NULL"<<std::endl;
				}
#endif
		}
		for (std::map<std::string, std::string>::iterator iter = properties.begin();
				iter != properties.end(); ++iter, i++) {
			sxn_environ->env->SetObjectArrayElement(stringArray, i,
					sxn_environ->env->NewStringUTF((iter->first).c_str()));
			sxn_environ->env->SetObjectArrayElement(objectArray, i,
					sxn_environ->env->NewStringUTF((iter->second).c_str()));
		}
	}
	jobject result = (jobject)(
			sxn_environ->env->CallObjectMethod(cppV, mID,
					sxn_environ->env->NewStringUTF(cwdV.c_str()),
					sxn_environ->env->NewStringUTF(sourceFile), stringArray, objectArray));
	if (size > 0) {
		sxn_environ->env->DeleteLocalRef(stringArray);
		sxn_environ->env->DeleteLocalRef(objectArray);
	}
	if (result) {
		XdmNode * node = new XdmNode(result);
		node->setProcessor(proc);
		return node;
	}

}
#ifdef DEBUG
	sxn_environ->env->ExceptionDescribe();
#endif
sxn_environ->env->ExceptionClear();

}

void SchemaValidator::exceptionClear(){
 if(proc->exception != NULL) {
 	delete proc->exception;
 	proc->exception = NULL;
 }
   sxn_environ->env->ExceptionClear();
 }

const char * SchemaValidator::getErrorCode(int i) {
	if(proc->exception == NULL) {return NULL;}
	return proc->exception->getErrorCode(i);
}

const char * SchemaValidator::getErrorMessage(int i ){
 if(proc->exception == NULL) {return NULL;}
 return proc->exception->getErrorMessage(i);
 }

bool SchemaValidator::exceptionOccurred() {
	return proc->exceptionOccurred() || proc->exception != NULL;
}

const char* SchemaValidator::checkException() {
	/*if(proc->exception == NULL) {
	 proc->exception = proc->checkForException(environ, cppClass, cpp);
	 }
	 return proc->exception;*/
	return checkForException(*(sxn_environ), cppClass, cppV);
}

int SchemaValidator::exceptionCount(){
 if(proc->exception != NULL){
 return proc->exception->count();
 }
 return 0;
 }

void SchemaValidator::setSourceNode(XdmNode * value) {
    if(value != NULL){
      value->incrementRefCount();
      parameters["node"] = (XdmValue *)value;
    }

	
}

void SchemaValidator::setParameter(const char * name, XdmValue*value) {
	if(value != NULL){
		value->incrementRefCount();
		parameters["param:"+std::string(name)] = value;
	}
}

bool SchemaValidator::removeParameter(const char * name) {
	return (bool)(parameters.erase("param:"+std::string(name)));
}

void SchemaValidator::setProperty(const char * name, const char * value) {
#ifdef DEBUG	
	if(value == NULL) {
		std::cerr<<"Validator setProperty is NULL"<<std::endl;
	}
#endif
	properties.insert(std::pair<std::string, std::string>(std::string(name), std::string((value== NULL ? "" : value))));
}

void SchemaValidator::clearParameters(bool delVal) {
	if(delVal){
       		for(std::map<std::string, XdmValue*>::iterator itr = parameters.begin(); itr != parameters.end(); itr++){
			XdmValue * value = itr->second;
			value->decrementRefCount();
#ifdef DEBUG
			std::cout<<"SchemaValidator.clearParameter() - XdmValue refCount="<<value->getRefCount()<<std::endl;
#endif
			if(value != NULL && value->getRefCount() < 1){		
	        		delete value;
			}
        	}
		parameters.clear();
	}
}

void SchemaValidator::clearProperties() {
	properties.clear();
}

std::map<std::string,XdmValue*>& SchemaValidator::getParameters(){
	std::map<std::string,XdmValue*>& ptr = parameters;
	return ptr;
}

std::map<std::string,std::string>& SchemaValidator::getProperties(){
	std::map<std::string,std::string> &ptr = properties;
	return ptr;
}

