//
//  ShareViewController.swift
//  Compose_ShareExtension
//
//  Created by Michael Dietrich on 21.08.24.
//  Copyright © 2024 orgName. All rights reserved.
//

import SwiftUI
import shared
import UniformTypeIdentifiers

class ShareViewController: UIViewController {

    override func viewDidAppear(_ animated: Bool) {
        guard let extensionItem = extensionContext?.inputItems.first as? NSExtensionItem,
              let itemProvider = extensionItem.attachments else {
            close()
            return
        }
        let sharedFolder: URL? = FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: "group.Compose.Wallet")
        let sharedFile = (sharedFolder?.appending(path: "share.pdf"))!

        
        for provider in itemProvider {
            if provider.hasItemConformingToTypeIdentifier(UTType.pdf.identifier) {
                provider.loadItem(forTypeIdentifier: UTType.pdf.identifier, options: nil) { data, error in
                    if let url = data as? NSURL {
                        do {
                            if FileManager.default.fileExists(atPath: sharedFile.absoluteString) {
                                try FileManager.default.removeItem(at: sharedFile)
                            }
                            try FileManager.default.copyItem(at: url as URL, to: sharedFile)
                        } catch {
                            print("error \(error)")
                        }
                        self.open(url: "wallet-pdf://\(sharedFile.absoluteString)")
                        self.close()
                    }
                }
            }
        }
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        DispatchQueue.main.async {
            let contentView = UIHostingController(rootView: ShareExtensionView())
            self.addChild(contentView)
            self.view.addSubview(contentView.view)
            
            contentView.view.translatesAutoresizingMaskIntoConstraints = false
            contentView.view.topAnchor.constraint(equalTo: self.view.topAnchor).isActive = true
            contentView.view.bottomAnchor.constraint (equalTo: self.view.bottomAnchor).isActive = true
            contentView.view.leftAnchor.constraint(equalTo: self.view.leftAnchor).isActive = true
            contentView.view.rightAnchor.constraint (equalTo: self.view.rightAnchor).isActive = true
        }
    }
    
    func open(url: String) {
        let url = NSURL(string:url)
        var responder = self as UIResponder?

        while (responder != nil){
            if responder?.responds(to: #selector(UIApplication.openURL(_:))) == true{
                responder?.perform(#selector(UIApplication.openURL(_:)), with: url)
            }
            responder = responder!.next
        }
    }
    
    func close() {
        self.extensionContext?.completeRequest(returningItems: [], completionHandler: nil)
    }
}
