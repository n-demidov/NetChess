package edu.demidov.netchess.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class XmlSerialization {

    private final static Logger log = LoggerFactory.getLogger(XmlSerialization.class);
    private static XmlSerialization instance;

    private XmlSerialization() {
    }

    public static synchronized XmlSerialization getInstance() {
        if (instance == null) {
            instance = new XmlSerialization();
        }
        return instance;
    }

    public synchronized void write(final Object w, final String fileName) throws FileNotFoundException {
        log.trace("write w={}, fileName={}", w, fileName);
        try (final XMLEncoder encoder = new XMLEncoder(
                new BufferedOutputStream(
                        new FileOutputStream(fileName)))) {
            encoder.writeObject(w);
        }
    }

    public synchronized Object read(final String fileName) throws FileNotFoundException {
        log.trace("read fileName={}", fileName);
        try (final XMLDecoder decoder = new XMLDecoder(
                new BufferedInputStream(
                        new FileInputStream(fileName)))) {
            return decoder.readObject();
        }
    }

}
