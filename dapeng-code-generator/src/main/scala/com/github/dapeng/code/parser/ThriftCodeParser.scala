package com.github.dapeng.code.parser

import java.io._
import java.util

import com.github.dapeng.core
import com.github.dapeng.core.metadata.Service.ServiceMeta
import com.github.dapeng.core.metadata.TEnum.EnumItem
import com.github.dapeng.core.metadata.{DataType, Method, TEnum}
import com.github.mustachejava.Mustache
import com.google.common.base.Charsets
import com.google.common.io.CharStreams
import com.github.dapeng.core.metadata
import com.github.dapeng.core.metadata.TEnum.EnumItem
import com.github.dapeng.core.metadata.{Annotation, DataType, Method, TEnum}
import com.twitter.scrooge.ast._
import com.twitter.scrooge.frontend.{Importer, ResolvedDocument, ThriftParser, TypeResolver}
import com.twitter.scrooge.java_generator._

import scala.collection.JavaConversions._
import scala.collection.concurrent.TrieMap
import scala.collection.mutable
import scala.util.control.Breaks._


/**
  * Thrift Code 解析器
  *
  * @author craneding
  * @date 15/7/22
  */
class ThriftCodeParser(var language: String) {

  private val templateCache = new TrieMap[String, Mustache]
  private val docCache = new mutable.HashMap[String, Document]()
  private val enumCache = new util.ArrayList[TEnum]()
  private val structCache = new util.ArrayList[core.metadata.Struct]()
  private val serviceCache = new util.ArrayList[core.metadata.Service]()

  private val mapStructCache = new util.HashMap[String, core.metadata.Struct]()
  private val mapEnumCache = new util.HashMap[String, TEnum]()

  /**
    * 生成文档
    *
    * @param resource 源文件
    * @return 文档
    */
  private def generateDoc(resource: String): Document = {

    val homeDir = resource.substring(0, if (resource.lastIndexOf("/") == -1) resource.lastIndexOf("\\") else resource.lastIndexOf("/"))

    val br = new BufferedReader(new InputStreamReader(new FileInputStream(resource), Charsets.UTF_8))
    val txt = CharStreams.toString(br)

    val importer = Importer(Seq(homeDir))
    val parser = new ThriftParser(importer, true)
    val doc = parser.parse(txt, parser.document)

    try {
      TypeResolver()(doc).document
    } finally {
      println(s"parse ${resource} success")
    }
  }

  /**
    * 获取生成器
    *
    * @param doc0        文档结构
    * @param genHashcode 是否生成HashCode
    * @return 生成器
    */
  private def getGenerator(doc0: Document, genHashcode: Boolean = false): ApacheJavaGenerator = {
    new ApacheJavaGenerator(new ResolvedDocument(doc0, new TypeResolver()), "thrift", templateCache, genHashcode = genHashcode)
    //new ApacheJavaGenerator(Map(), "thrift", templateCache, genHashcode = genHashcode)
  }

  private def toDocString(docstring: scala.Option[String]): String =
    if (docstring == None)
      null
    else {
      val result = docstring.get.toString();

      val in = new BufferedReader(new StringReader(result));

      val buffer = new StringBuilder();

      breakable {
        while (true) {
          var line = in.readLine();

          if (line == null)
            break;

          //          line = line.trim();

          if (line.matches("^\\s*[*]{1,2}/$")) {
            line = ""
          } else if (line.matches("^\\s*[*].*$")) {
            line = line.trim();
            line = line.substring(1)
          } else if (line.matches("^\\s*/[*]{2}.*$")) {
            line = line.trim();
            line = line.substring("/**".length)
          }

          if (line.endsWith("**/"))
            line = line.substring(0, line.lastIndexOf("**/"))
          if (line.endsWith("*/"))
            line = line.substring(0, line.lastIndexOf("*/"))

          if (line.length > 0) {
            if (buffer.length > 0)
              buffer.append("\n")
            buffer.append(line);
          } else {
            buffer.append("\n");
          }
        }
      }

      in.close()

      //result = result.replace("/**", "").replace("**/", "").replace("*/", "").trim;
      //val pattern = "[ ]*[*][\\s]?".r;
      //result = (pattern replaceAllIn(result, ""));

      return buffer.toString();
    }


  private def toDataType(fieldType: FieldType, defaultDoc: Document, docString: String): DataType = {
    val dataType = new DataType()

    fieldType match {
      case _: BaseType =>
        val clazz = fieldType.getClass

        if (clazz == TI16.getClass) {
          dataType.setKind(DataType.KIND.SHORT)
        } else if (clazz == TI32.getClass) {
          dataType.setKind(DataType.KIND.INTEGER)
        } else if (clazz == TI64.getClass) {
          dataType.setKind(DataType.KIND.LONG)

          //2016-2-18 In order to generate Date type
          if (docString != null && docString.toLowerCase.contains("@datatype(name=\"date\")"))
            dataType.setKind(DataType.KIND.DATE)

        } else if (clazz == TDouble.getClass) {
          dataType.setKind(DataType.KIND.DOUBLE)

          //2016-4-08 In order to generate BigDecimal type
          if (docString != null && docString.toLowerCase.contains("@datatype(name=\"bigdecimal\")"))
            dataType.setKind(DataType.KIND.BIGDECIMAL)

        } else if (clazz == TByte.getClass) {
          dataType.setKind(DataType.KIND.BYTE)
        } else if (clazz == TBool.getClass) {
          dataType.setKind(DataType.KIND.BOOLEAN)
        } else if (clazz == TString.getClass) {
          dataType.setKind(DataType.KIND.STRING)
        } else {
          dataType.setKind(DataType.KIND.BINARY)
        }
      case EnumType(enum, scopePrefix) =>
        dataType.setKind(DataType.KIND.ENUM)

        val doc1 = if (scopePrefix != None) docCache(scopePrefix.get.name) else defaultDoc
        val enumController = new EnumController(enum, getGenerator(doc1), getNameSpace(doc1, language))

        dataType.setQualifiedName(enumController.namespace + "." + enumController.name)

      case StructType(struct, scopePrefix) =>
        dataType.setKind(DataType.KIND.STRUCT)

        val doc1 = if (scopePrefix != None) docCache(scopePrefix.get.name) else defaultDoc
        val structController = new StructController(struct, false, getGenerator(doc1), getNameSpace(doc1, language))

        dataType.setQualifiedName(structController.namespace + "." + structController.name)
      case _: ListType =>
        dataType.setKind(DataType.KIND.LIST)

        dataType.setValueType(toDataType(fieldType.asInstanceOf[ListType].eltType, defaultDoc, docString))
      case _: SetType =>
        dataType.setKind(DataType.KIND.SET)

        dataType.setValueType(toDataType(fieldType.asInstanceOf[SetType].eltType, defaultDoc, docString))
      case _: MapType =>
        dataType.setKind(DataType.KIND.MAP)

        dataType.setKeyType(toDataType(fieldType.asInstanceOf[MapType].keyType, defaultDoc, docString))
        dataType.setValueType(toDataType(fieldType.asInstanceOf[MapType].valueType, defaultDoc, docString))
      case _ =>
        dataType.setKind(DataType.KIND.VOID)
    }

    dataType
  }

  private def getNameSpace(doc: Document, language:String): Option[Identifier] = {
    doc.namespace(language) match {
      case x@Some(id) => x
      case None =>  // return a default namespace of java
        doc.namespace("java")
    }

  }

  private def findEnums(doc: Document, generator: ApacheJavaGenerator): util.List[TEnum] = {
    val results = new util.ArrayList[TEnum]()

    doc.enums.foreach(e => {
      val controller = new EnumController(e, generator, getNameSpace(doc, language))

      val tenum = new TEnum()
      if (controller.has_namespace)
        tenum.setNamespace(controller.namespace)

      if (e.annotations.size > 0)
        tenum.setAnnotations(e.annotations.map { case (key, value) => new Annotation(key, value) }.toList)

      tenum.setName(controller.name)
      tenum.setDoc(toDocString(e.docstring))
      tenum.setEnumItems(new util.ArrayList[EnumItem]())

      for (index <- (0 until controller.constants.size)) {
        val enumFiled = controller.constants(index)

        val name = enumFiled.name
        val value = enumFiled.value.toString.toInt
        val docString = toDocString(e.values(index).docstring)

        val enumItem = new EnumItem()
        enumItem.setLabel(name)
        enumItem.setValue(value)
        enumItem.setDoc(docString)

        val teItem = e.values.get(index)
        if (teItem.annotations.size > 0)
          enumItem.setAnnotations(teItem.annotations.map { case (key, value) => new Annotation(key, value) }.toList)
        tenum.getEnumItems.add(enumItem)
      }

      results.add(tenum)
    })

    results
  }

  private def findStructs(doc0: Document, generator: ApacheJavaGenerator): List[core.metadata.Struct] =
    doc0.structs.toList.map { (struct: StructLike) =>
      val controller = new StructController(struct, false, generator, getNameSpace(doc0, language))

      new core.metadata.Struct {
        //this.setNamespace(if (controller.has_non_nullable_fields) controller.namespace else null)
        this.setNamespace(controller.namespace)
        this.setName(controller.name)
        this.setDoc(toDocString(struct.docstring))

        if (struct.annotations.size > 0)
          this.setAnnotations(struct.annotations.map { case (key, value) => new Annotation(key, value) }.toList)

        val fields0 = controller.allFields.zip(controller.fields).toList.map { case (field, fieldController) =>
          val tag0 = field.index.toString.toInt
          val name0 = field.originalName
          //val optional0 = fieldController.optional_or_nullable.toString.toBoolean
          val optional0 = field.requiredness.isOptional
          val docSrting0 = toDocString(field.docstring)
          var dataType0: DataType = null

          dataType0 = toDataType(field.fieldType, doc0, docSrting0)

          new core.metadata.Field {
            this.setTag(tag0)
            this.setName(name0)
            this.setOptional(optional0)
            this.setDoc(docSrting0)
            this.setDataType(dataType0)
            this.setPrivacy(false)

            if (field.fieldAnnotations.size > 0)
              this.setAnnotations(field.fieldAnnotations.map { case (key, value) => new Annotation(key, value) }.toList)

          }
        }

        this.setFields(fields0)
      }
    }


  private def findServices(doc: Document, generator: ApacheJavaGenerator): util.List[core.metadata.Service] = {
    val results = new util.ArrayList[core.metadata.Service]()

    doc.services.foreach(s => {
      val controller = new ServiceController(s, generator, getNameSpace(doc, language))

      val service = new core.metadata.Service()

      service.setNamespace(if (controller.has_namespace) controller.namespace else null)
      service.setName(controller.name)
      service.setDoc(toDocString(s.docstring))
      if (s.annotations.size > 0)
        service.setAnnotations(s.annotations.map { case (key, value) => new Annotation(key, value) }.toList)

      val methods = new util.ArrayList[Method]()
      for (tmpIndex <- (0 until controller.functions.size)) {
        val functionField = controller.functions(tmpIndex)
        //controller.functions.foreach(functionField => {
        val request = new core.metadata.Struct()
        val response = new core.metadata.Struct()

        request.setName(functionField.name + "_args")
        response.setName(functionField.name + "_result")

        val method = new Method()
        method.setName(functionField.name)
        method.setRequest(request)
        method.setResponse(response)
        method.setDoc(toDocString(s.functions(tmpIndex).docstring))

        if (s.functions(tmpIndex).annotations.size > 0)
          method.setAnnotations(s.functions(tmpIndex).annotations.map { case (k, v) => new Annotation(k, v) }.toList)

        if (method.getDoc != null && method.getDoc.contains("@IsSoaTransactionProcess"))
          method.setSoaTransactionProcess(true)
        else
          method.setSoaTransactionProcess(false)

        request.setFields(new util.ArrayList[core.metadata.Field]())
        response.setFields(new util.ArrayList[core.metadata.Field]())

        for (index <- (0 until functionField.fields.size)) {
          val field = functionField.fields(index)

          val realField = s.functions.get(tmpIndex).args.get(index)

          val tag = index + 1
          val name = field.name

          val docSrting = if (realField.docstring.isDefined) toDocString(realField.docstring) else ""

          val f = field.field_type.getClass.getDeclaredField("fieldType");
          f.setAccessible(true)
          val dataType = toDataType(f.get(field.field_type).asInstanceOf[FieldType], doc, docSrting)

          val tfiled = new core.metadata.Field()
          tfiled.setTag(tag)
          tfiled.setName(name)
          tfiled.setDoc(docSrting)
          tfiled.setDataType(dataType)
          tfiled.setOptional(field.optional)
          if (realField.fieldAnnotations.size > 0)
            tfiled.setAnnotations(realField.fieldAnnotations.map { case (k, v) => new Annotation(k, v) }.toList)
          request.getFields.add(tfiled)
        }

        val docSrting = if (s.functions.get(tmpIndex).docstring.isDefined) toDocString(s.functions.get(tmpIndex).docstring) else ""

        val f = functionField.return_type.getClass.getDeclaredField("fieldType")
        f.setAccessible(true)

        var dataType: DataType = null
        if (f.get(functionField.return_type).getClass == com.twitter.scrooge.ast.Void.getClass) {
          dataType = new DataType()
          dataType.setKind(DataType.KIND.VOID)
        } else {
          dataType = toDataType(f.get(functionField.return_type).asInstanceOf[FieldType], doc, docSrting)
        }

        val tfiled = new core.metadata.Field()
        tfiled.setTag(0)
        tfiled.setName("success")
        tfiled.setDoc(docSrting)
        tfiled.setDataType(dataType)
        tfiled.setOptional(false)
        response.getFields.add(tfiled)

        methods.add(method)
      }

      service.setMethods(methods)

      results.add(service)
    })

    results
  }

  def getAllStructs(resources: Array[String]): util.List[core.metadata.Struct] = {
    resources.foreach(resource => {
      val doc = generateDoc(resource)
      docCache.put(resource.substring(resource.lastIndexOf(File.separator) + 1, resource.lastIndexOf(".")), doc)
    })

    docCache.values.foreach(doc => {
      val generator = getGenerator(doc)
      structCache.addAll(findStructs(doc, generator))
    })
    structCache.toList
  }

  def getAllEnums(resources: Array[String]): util.List[TEnum] = {
    resources.foreach(resource => {
      val doc = generateDoc(resource)
      docCache.put(resource.substring(resource.lastIndexOf(File.separator) + 1, resource.lastIndexOf(".")), doc)
    })

    docCache.values.foreach(doc => {
      val generator = getGenerator(doc)
      enumCache.addAll(findEnums(doc, generator))
    })
    enumCache.toList
  }

  def toServices(resources: Array[String], serviceVersion: String): util.List[core.metadata.Service] = {
    resources.foreach(resource => {
      val doc = generateDoc(resource)

      docCache.put(resource.substring(resource.lastIndexOf(File.separator) + 1, resource.lastIndexOf(".")), doc)
    })

    docCache.values.foreach(doc => {
      val generator = getGenerator(doc)

      enumCache.addAll(findEnums(doc, generator))
      structCache.addAll(findStructs(doc, generator))
      serviceCache.addAll(findServices(doc, generator))

      for (enum <- enumCache)
        mapEnumCache.put(enum.getNamespace + "." + enum.getName, enum)
      for (struct <- structCache)
        mapStructCache.put(struct.getNamespace + "." + struct.getName, struct)
    })

    for (index <- (0 until serviceCache.size())) {
      val service = serviceCache.get(index)

      val structSet = new util.HashSet[core.metadata.Struct]()
      val enumSet = new util.HashSet[TEnum]()
      //递归将service中所有method的所有用到的struct加入列表
      val loadedStructs = new util.HashSet[String]()
      for (method <- service.getMethods) {
        for (field <- method.getRequest.getFields) {
          getAllStructs(field.getDataType, structSet)
          getAllEnums(field.getDataType, enumSet, loadedStructs)
        }
        for (field <- method.getResponse.getFields) {
          getAllStructs(field.getDataType, structSet)
          getAllEnums(field.getDataType, enumSet, loadedStructs)
        }
      }
      service.setStructDefinitions(structSet.toList)
      service.setEnumDefinitions(enumSet.toList)
      //      service.setEnumDefinitions(enumCache)
      //      service.setStructDefinitions(structCache)
      service.setMeta(new ServiceMeta {
        if (serviceVersion != null && !serviceVersion.trim.equals(""))
          this.version = serviceVersion.trim
        else
          this.version = "1.0.0"
        this.timeout = 30000
      })
    }

    return serviceCache;
  }

  /**
    * 递归添加所有struct
    *
    * @param dataType
    * @param structSet
    */
  def getAllStructs(dataType: metadata.DataType, structSet: java.util.HashSet[metadata.Struct]): Unit = {

    if (dataType.getKind == DataType.KIND.STRUCT) {
      val struct = mapStructCache.get(dataType.getQualifiedName)

      if (structSet.contains(struct))
        return

      structSet.add(struct)

      for (tmpField <- struct.getFields) {
        getAllStructs(tmpField.getDataType, structSet)
      }
    }
    else if (dataType.getKind == DataType.KIND.SET || dataType.getKind == DataType.KIND.LIST) {
      getAllStructs(dataType.getValueType, structSet)

    } else if (dataType.getKind == DataType.KIND.MAP) {
      getAllStructs(dataType.getKeyType, structSet)
      getAllStructs(dataType.getValueType, structSet)
    }
  }

  /**
    * 递归添加所有enum
    *
    * @param dataType
    * @param enumSet
    */
  def getAllEnums(dataType: metadata.DataType, enumSet: util.HashSet[metadata.TEnum], loadedStructs: util.HashSet[String]): Unit = {

    if (dataType.getKind == DataType.KIND.ENUM)
      enumSet.add(mapEnumCache.get(dataType.getQualifiedName))

    else if (dataType.getKind == DataType.KIND.STRUCT) {

      if (loadedStructs.contains(dataType.getQualifiedName))
        return

      loadedStructs.add(dataType.getQualifiedName)

      val struct = mapStructCache.get(dataType.getQualifiedName)

      for (field <- struct.getFields)
        getAllEnums(field.getDataType, enumSet, loadedStructs)
    }
    else if (dataType.getKind == DataType.KIND.SET || dataType.getKind == DataType.KIND.LIST) {
      getAllEnums(dataType.getValueType, enumSet, loadedStructs)

    } else if (dataType.getKind == DataType.KIND.MAP) {
      getAllEnums(dataType.getKeyType, enumSet, loadedStructs)
      getAllEnums(dataType.getValueType, enumSet, loadedStructs)
    }
  }
}
