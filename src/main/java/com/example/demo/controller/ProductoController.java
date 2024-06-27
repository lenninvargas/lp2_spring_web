package com.example.demo.controller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.entity.DetallePedidoEntity;
import com.example.demo.entity.ProductoEntity;
import com.example.demo.entity.UsuarioEntity;
import com.example.demo.model.Pedido;
import com.example.demo.service.ProductoService;
import com.example.demo.service.UsuarioService;
import com.example.demo.service.impl.PdfService;

import jakarta.servlet.http.HttpSession;

@Controller
public class ProductoController {
	
	@Autowired
	private UsuarioService usuarioService;
	
	@Autowired
	private ProductoService productoService;
	
	@Autowired
	private PdfService pdfService;
	
	@GetMapping("/menu")
	public String showMenu(HttpSession session, Model model) {
		if(session.getAttribute("usuario") == null) {
			return "redirect:/";
		}
		
		String correo = session.getAttribute("usuario").toString();
		UsuarioEntity usuarioEntity = usuarioService.buscarUsuarioPorCorreo(correo);
		model.addAttribute("foto", usuarioEntity.getUrlImagen());
		
		List<ProductoEntity>productos = productoService.buscarTodosProductos();
		model.addAttribute("productos", productos);
		
		//numero pedido
		List<Pedido> productoSession = session.getAttribute("carrito") == null ? new ArrayList<Pedido>() : (List<Pedido>) session.getAttribute("carrito");
		
		/*List<Pedido>productoSession = null;
		if(session.getAttribute("carrito") == null) {
			productoSession = new ArrayList<Pedido>();
		}else {
			productoSession = (List<Pedido>) session.getAttribute("carrito");
		}*/
		model.addAttribute("cant_carrito", productoSession.size());
		
		// ver carrito con datos
		List<DetallePedidoEntity> detallePedidoEntityList = new ArrayList<DetallePedidoEntity>();
		Double total = 0.0;
		
		for(Pedido pedido: productoSession) {
			DetallePedidoEntity detallePedidoEntity = new DetallePedidoEntity();
			ProductoEntity productoEntity = productoService.buscarProductoPorId(pedido.getProductoId());
			detallePedidoEntity.setProductoEntity(productoEntity);
			detallePedidoEntity.setCantidad(pedido.getCantidad());
			detallePedidoEntityList.add(detallePedidoEntity);
			total += pedido.getCantidad() * productoEntity.getPrecio();
		}
		model.addAttribute("carrito", detallePedidoEntityList);
		model.addAttribute("total", total);
		
											
		return "menu";
	}
	
	@PostMapping("/agregar_producto")
	public String agregarProducto(HttpSession session,@RequestParam("prodId") String prod,
			@RequestParam("cant") String cant) {
		
		List<Pedido> productos = null;
		if(session.getAttribute("carrito") == null) {
			productos = new ArrayList<>();
		}else {
			productos = (List<Pedido>) session.getAttribute("carrito");
		}
		
		Integer cantidad = Integer.parseInt(cant);
		Integer prodId = Integer.parseInt(prod);
		Pedido pedido = new Pedido(cantidad, prodId);
		productos.add(pedido);
		session.setAttribute("carrito", productos);
		return "redirect:/menu";
		
	}
	
	@GetMapping("/generar_pdf")
	public ResponseEntity<InputStreamResource>generarPdf(HttpSession session) throws IOException{
		List<Pedido>productoSession = null;
		if(session.getAttribute("carrito") == null) {
			productoSession = new ArrayList<Pedido>();
		}else {
			productoSession = (List<Pedido>) session.getAttribute("carrito");
		}
		List<DetallePedidoEntity> detallePedidoEntityList = new ArrayList<DetallePedidoEntity>();
		Double total = 0.0;
		
		for(Pedido pedido: productoSession) {
			DetallePedidoEntity detallePedidoEntity = new DetallePedidoEntity();
			ProductoEntity productoEntity = productoService.buscarProductoPorId(pedido.getProductoId());
			detallePedidoEntity.setProductoEntity(productoEntity);
			detallePedidoEntity.setCantidad(pedido.getCantidad());
			detallePedidoEntityList.add(detallePedidoEntity);
			total += pedido.getCantidad() * productoEntity.getPrecio();
		}
		
		Map<String, Object>datosPdf = new HashMap<String, Object>();
		datosPdf.put("factura", detallePedidoEntityList);
		datosPdf.put("precio_total", total);
		
		ByteArrayInputStream pdfBytes = pdfService.generarPdfDeHtml("template_pdf", datosPdf);
		
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.add("Content-Disposition", "inline; filename=productos.pdf");
		
		return ResponseEntity.ok()
				.headers(httpHeaders)
				.contentType(MediaType.APPLICATION_PDF)
				.body(new InputStreamResource(pdfBytes));
	}
}
